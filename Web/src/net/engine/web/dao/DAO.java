package net.engine.web.dao;


import java.util.*;

import javax.servlet.ServletConfig;

public class DAO 
{
	//private static final NO_OF_FRAGS = 10;
	private int dbFragments;
	private int poolSize;
	private String driverClassName;
	//private String urls[],user[],pwd[];
	
	//private ArrayList connectionObjects;
	private ArrayList databaseFragments;

	
	public DAO(ServletConfig config) throws ClassNotFoundException
	{
		String url="",user="",pwd="";
		
		dbFragments = Integer.parseInt(config.getInitParameter("dbFragments"));
		poolSize = Integer.parseInt(config.getInitParameter("poolSize"));
		driverClassName = config.getInitParameter("driverClassName");
		
		Class.forName(driverClassName);
		
		databaseFragments = new ArrayList(dbFragments);
		
		if (dbFragments > 26)
		{
			throw new RuntimeException("No. of Database Fragments should not be set to more than 26");
		}
				
		/*frag1_url , frag1_user, frag1_pwd
		 *frag2_url , frag2_user, frag2_pwd
		 */
		
		for (int i =0 ; i < dbFragments;i++)
		{
			url = config.getInitParameter("frag"+i+"_url");
			user = config.getInitParameter("frag"+i+"_user");
			pwd = config.getInitParameter("frag"+i+"_pwd");
			
			DatabaseFragment df = new DatabaseFragment(this,(char)('A' + i),url,user,pwd,poolSize);
			//df.setDAOCallback(this);
			databaseFragments.add(df);
		}		
	}
	
	public ArrayList query(String searchTerm)
	{
		HashMap personalResultsCache = new HashMap();		
		ArrayList links = wordExists(searchTerm);
		ArrayList results = new ArrayList();
		if (links.size() > 0 )
		{
			// some links exist against this word.
			//submitQuery(searchTerm);
			searchLink(searchTerm,links, personalResultsCache);
			// TODO : GET RESULTS IS DEFUNCT. RETRIEVE FROM PERSONALRESULTSCACHE
			//return getResults(searchTerm);
			
			synchronized (personalResultsCache)
			{
				int resultsPending = Integer.parseInt((String)personalResultsCache.get("ResultsPending"));
				while (resultsPending > 0)
				{
					try 
					{
						personalResultsCache.wait();
					} catch(InterruptedException ie) { ie.printStackTrace(); }
					resultsPending = Integer.parseInt((String)personalResultsCache.get("ResultsPending"));
				}
			}

			
			Iterator keyFragItr = personalResultsCache.keySet().iterator();
			while (keyFragItr.hasNext())
			{
				String f = (String)keyFragItr.next();
				if (f.equals("ResultsPending")) continue;
				ArrayList res = (ArrayList) personalResultsCache.get(f);		
				results.addAll(res);
			}
		}
		return results;
	}
	
	/*Request would be sent to one specific database fragment that 
	 *is supposed to contain the word ( if the word exists)
	 */
	private ArrayList wordExists(String searchTerm)
	{
		// range of fragID is [0-25]
		int fragID = searchTerm.hashCode() % dbFragments;
				
		HashMap resultsCache = new HashMap();
		ArrayList qry = new ArrayList();
		qry.add(searchTerm);
		resultsCache.put(String.valueOf((char)(fragID + 'A')),qry);
		// because we are searching for only 1 word.
		resultsCache.put("ResultsPending","1"); 
		
		DatabaseFragment dfrag = (DatabaseFragment)databaseFragments.get(fragID);
		dfrag.addWordSearchRequest(searchTerm,resultsCache);
		
		synchronized (resultsCache)
		{
			int resultsPending = Integer.parseInt((String)resultsCache.get("ResultsPending"));
			while (resultsPending > 0)
			{
				try 
				{
					resultsCache.wait();
				} catch(InterruptedException ie) { ie.printStackTrace(); }
				resultsPending = Integer.parseInt((String)resultsCache.get("ResultsPending"));
			}
		}
		ArrayList result = (ArrayList)resultsCache.get(String.valueOf((char)(fragID + 'A')));
		return result;
	}
	
	
	/*This query will broadcast the search for the link rowid to the
	 *respective database fragments.
	 *from each link row it will find which database frag to call to
	 *then retrieve from that link. e.g :
	 *input -> ROWID = A0X1J2J3
	 *then first char ( 'A' ) determines the frag. no ( we can't have 
	 *more than 26 fragments!). [A-Z] range.
	 */
	private void searchLink(String searchTerm,ArrayList linkid,HashMap resultsCache)
	{
		int limit  = linkid.size();
		
		//resultsCache.put("ResultsPending","0");
		
		/*HashMap stores: DB Fragment ID , rows required from there.
		 *["ResultsPending", stores number of frags from where results are expected.]
		 *["A" , ArrayList: {rowid1,rowid2,rowid3,rowid4...}]
		 *["B" , ArrayList: {rowid1,rowid2,rowid3,rowid4...}]
		 *...
		 *["Z" , ArrayList: {rowid1,rowid2,rowid3,rowid4...}]
		 */
		for (int i =0 ; i < limit ; i++)
		{
			String rowid = (String)linkid.get(i);
			String idx = String.valueOf(rowid.charAt(0));
			ArrayList rowidList = (ArrayList)resultsCache.get(idx);
			if (rowidList == null)
			{
				rowidList = new ArrayList();
				resultsCache.put(idx,rowidList);
			}
			rowidList.add(rowid.substring(1));
		}

		int size = resultsCache.size();
		resultsCache.put("ResultsPending",String.valueOf(size));
		//linkid.clear();
		
		Iterator keyFragItr = resultsCache.keySet().iterator();
		while (keyFragItr.hasNext())
		{
			String f = (String)keyFragItr.next();
			if ( f.equals("ResultsPending")) continue;
			
			ArrayList qry = (ArrayList) resultsCache.get(f);
			int fidx = f.charAt(0) - 'A';
			DatabaseFragment dfrag = (DatabaseFragment)databaseFragments.get(fidx);
			/*Pass entire resultsCache to every participating
			 *db fragment. They will modify the corresponding 
			 *entry which has queries for their fragment and put
			 *results into that entry.
			 */
			dfrag.addLinkSearchRequest(searchTerm,resultsCache);
		}
	}
	/*
	private ArrayList getResults(String searchTerm)
	{
		int limit  = databaseFragments.size();
		ArrayList hits = new ArrayList();
		for ( int i = 0 ;i<limit;i++)
		{
			DatabaseFragment dfrag = (DatabaseFragment)databaseFragments.get(i);
			ArrayList res = dfrag.resultsVisitor(searchTerm);
			hits.addAll(res);
		}
		return hits;		
	}
	*/
}
