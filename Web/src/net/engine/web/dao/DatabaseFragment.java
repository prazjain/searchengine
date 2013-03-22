package net.engine.web.dao;

import java.sql.*;
import java.util.*;

public class DatabaseFragment {
	
	private int poolSize;
	private Connection dbConnection;
	private ThreadPool wordSearchPool;
	private ThreadPool linkSearchPool;
	//private ArrayList searchQueue; /* [searchTerm] */
	private ArrayList wordSearchQueue;/* [searchTerm] */
	private ArrayList linkSearchQueue;/* [searchTerm] */
	private HashMap wordSearchWaitThreads; /* [searchTerm, ResultWaitList] */
	private HashMap linkSearchWaitThreads; /* [searchTerm, ResultWaitList{HashMap : resultsCache1,resultsCache2...}] */
	
	/*Reason that 2 wordSearchWaitThreads, linkSearchWaitThreads are 
	 *kept, is because suppose one group of requests are in first
	 *phase of wordSearch, and another group is in second phase of 
	 *linkSearch so they should not overlap each other.
	 */
	
	private DAO daoCallback;

//	private Hashtable results; /* [searchTerm, ArrayList] */
	private Hashtable wordResults; /* [searchTerm, ArrayList(rowid)] */
	private Hashtable linkResults; /* [searchTerm, ArrayList(actual links)] */
	
	
	private String dFragId;
	
	
	//private String wordQuery = "select rid  from wordlinkmap where wordid=? ";
	//private String linkQuery = "select link from linkbank where rowid='?' ";
	
	public static final int WORD_QUERY=1;
	public static final int LINK_QUERY=2;
		
	public DatabaseFragment(DAO callback,char id,String url,String user,String pwd,int poolSize)
	{
		try 
		{
			daoCallback = callback;
			dFragId = String.valueOf(id);
			dbConnection = DriverManager.getConnection(url,user,pwd);
			//searchQueue = new ArrayList();
			//results = new Hashtable();
			wordResults = new Hashtable();
			linkResults = new Hashtable();
			wordSearchQueue = new ArrayList();
			linkSearchQueue = new ArrayList();
			wordSearchWaitThreads = new HashMap();
			linkSearchWaitThreads = new HashMap();
			//threadPool = new ThreadPool(dbConnection,this,poolSize);
			wordSearchPool = new ThreadPool(dbConnection,this,poolSize,WORD_QUERY);
			linkSearchPool = new ThreadPool(dbConnection,this,poolSize,LINK_QUERY);
		}
		catch(SQLException sqe)
		{
			//log it
			sqe.printStackTrace();
		}
	}
	/*
	public void setDAOCallback(DAO callback)
	{
		daoCallback = callback;
	}
	*/
	/*called by client/end user threads to submit their 
	 *search terms to this datafragment.
	 */	
	public void addWordSearchRequest(String searchTerm,HashMap resultsCache)
	{
		ResultWaitList rwlist;
		synchronized (wordSearchQueue)
		{
			
			rwlist = (ResultWaitList)wordSearchWaitThreads.get(searchTerm);
			if (rwlist == null)
			{
				// new searchTerm
				rwlist = new ResultWaitList ();
				wordSearchQueue.add(searchTerm);
				wordSearchWaitThreads.put(searchTerm,rwlist);
			}
			
			// should i use sessions here instead of current thread?
			rwlist.addSearchRequest(resultsCache);
	
			wordSearchQueue.notifyAll();	// would notify waiting workerthreads on wordSearchQueue
		}	
	}
	
	
	/*called by client/end user threads to submit their 
	 *search terms to this datafragment.
	 */	
	public void addLinkSearchRequest(String searchTerm,HashMap resultsCache)
	{
		ResultWaitList rwlist;
		
		synchronized (linkSearchQueue)
		{
			rwlist = (ResultWaitList)linkSearchWaitThreads.get(searchTerm);
			if (rwlist == null)
			{
				// new searchTerm
				rwlist = new ResultWaitList ();
				linkSearchQueue.add(searchTerm);
				linkSearchWaitThreads.put(searchTerm,rwlist);
			}
			
			// should i use sessions here instead of current thread?
			rwlist.addSearchRequest(resultsCache);
	
			linkSearchQueue.notifyAll();	// would notify waiting workerthreads
		}
		
	//	return rwlist;
	}

	public Work getWork(int wtQuery)//, PreparedStatement ps)
	{
		String searchTerm;
		if (wtQuery == WORD_QUERY)
		{
			
			synchronized (wordSearchQueue)
			{
				//while(searchQueue.size() <=0 || results.containsKey(searchQueue.get(0)))
				while(wordSearchQueue.size() <=0 )
				{
					try 
					{
						wordSearchQueue.wait();
					} catch(InterruptedException ie) { ie.printStackTrace(); }
				}
				searchTerm = (String)wordSearchQueue.remove(0);
			}
			Work wrk = new Work (searchTerm,searchTerm);
			return wrk;			
		}
		else
		{
			//if (wtQuery.equals(linkQuery))
			synchronized (linkSearchQueue)
			{
				//while(searchQueue.size() <=0 || results.containsKey(searchQueue.get(0)))
				while(linkSearchQueue.size() <=0 )
				{
					try 
					{
						linkSearchQueue.wait();
					} catch(InterruptedException ie) { ie.printStackTrace(); }
				}
				searchTerm = (String)linkSearchQueue.remove(0);
			}
			ResultWaitList rwl = (ResultWaitList)linkSearchWaitThreads.get(searchTerm);
			ArrayList rowidlist =  (ArrayList)rwl.getQueryParams(dFragId);
			Work wrk = new BatchWork(searchTerm,rowidlist);
			return wrk;
		}
		
	}
	
/*	
	// called by WorkerThreads
	public String getWork()
	{
		synchronized (searchQueue)
		{
			//while(searchQueue.size() <=0 || results.containsKey(searchQueue.get(0)))
			while(searchQueue.size() <=0 )
				wait();
		}
		return searchQueue.remove(0);
	}
*/
	
	/*results are submitted to DatabaseFragment through this method by WorkerThreads
	 */
/*
	public void submitResult(String searchTerm,ResultSet rs)
	{
		ArrayList hits = new ArrayList();
		results.put(searchTerm,hits);
				
		/*if database frag doesnt have the record
		if (rs == null) return;
		

		//ResultSet rs = (ResultSet) results.get(searchTerm);
		while (rs.next())
		{
			String link = rs.getString("link");
			hits.add(link);
		}

		ResultWaitList rwl = (ResultWaitList) searchWaitThreads.get(searchTerm);
		synchronized (rwl)
		{
			rwl.setResultsAvailable(true);
			notifyAll() ; //	 notify all waiting threads 			
		}
	}
*/	
	public void submitResult(String searchTerm,int qType,ArrayList result)
	{
		if (qType == LINK_QUERY)
		{
			ResultWaitList rwl = (ResultWaitList)linkSearchWaitThreads.get(searchTerm);
			rwl.setResults(dFragId,result);
		}
		else
		{
			// if its a word query
			ResultWaitList rwl = (ResultWaitList) wordSearchWaitThreads.get(searchTerm);
			rwl.setResults(dFragId,result);			
		}
	}
	/*Would wait/block the end user threads that have come in before their 
	 *results have been retrieved by this database fragment. For others this 
	 *method would return the result, and decrement this thread from the 
	 *resultwaitlist object corresponding its searchTerm.
	 */
	 /*
	public ArrayList resultsVisitor(String searchTerm)
	{
		ResultWaitList rwl = (ResultWaitList)searchWaitThreads.get(searchTerm);
		synchronized (rwl)
		{
			while (! rwl.isResultsAvailable())
			{
				wait();
			}	
		}		
		rwl.removeThread(Thread.currentThread());
		ArrayList hits = (ArrayList) results.get(searchTerm);
		synchronized (rwl)
		{
			if (rwl.getSize() == 0)
			{
				// clean up the results hashtable
				results.remove(searchTerm);
			}
		}		
		return hits;
	}
	*/
}
