package net.engine.crawler;


import java.sql.*;
import java.util.*;
import java.io.*;

public class DebugDB {
	
	ArrayList con ;
	PreparedStatement linkUpdater;
	PreparedStatement linkIdRetriever;
	PreparedStatement wordUpdater;
	PreparedStatement wordLinkMapper;
	
	String dFragCode = "A";
	
	//BufferedWriter bw ;
	
	
	
	/**
	 * Method DebugDB
	 */
	public DebugDB() {
		try {
		/*
		Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		*/
		
		con = new ArrayList();
		/*
		con.add(DriverManager.getConnection(
			"jdbc:odbc:project"));
		*/
		
		Class.forName("oracle.jdbc.OracleDriver");
		con.add(DriverManager.getConnection("jdbc:oracle:thin:@172.18.18.148:1521:SCT","scott","tiger"));
		
		int i=0;
		Connection c = (Connection)con.get(i);
		/*
		linkUpdater = c.prepareStatement("insert into linkbank values('?')");
		linkIdRetriever = c.prepareStatement("select rowid from linkbank where link='?'");
		wordUpdater = c.prepareStatement("insert into wordbank (wordid,word) values (?,'?')");
		wordLinkMapper = c.prepareStatement("insert into wordlinkmap (wordid,rid) values(?,'?')");		
		*/
		linkUpdater = c.prepareStatement("insert into linkbank values(?)");
		linkIdRetriever = c.prepareStatement("select rowid from linkbank where link=?");
		wordUpdater = c.prepareStatement("insert into wordbank (wordid,word) values (?,?)");
		wordLinkMapper = c.prepareStatement("insert into wordlinkmap (wordid,rid) values(?,?)");		
		//bw = new BufferedWriter(new FileWriter("words.txt"));
		}
		catch(Exception e) { e.printStackTrace(); }		
	}	
	
	
	public int update(HashMap map)
	{
		int rows=0;
		int linksAdded = 0;
		int wordsAdded = 0;
		int wordLinkMappingAdded = 0;
		try
		{		
			Iterator itr = map.keySet().iterator();

		while (itr.hasNext())
		{
			String url = (String) itr.next();
			String words = (String)map.get(url);
			
			if (url==null || words==null)
			{
				System.out.println("url = "+url + " ; words = "+words);
				 continue;
			}
			
		//	if (url.trim().equals("") || words.trim().equals("")) continue;
			else
			{

				System.out.println(url);
				System.out.println(words);
/*
//				ps.clearParameters();
				ps.setString(1,url);
				ps.setString(2,words);
				rows += ps.executeUpdate();
			
				Statement s = con.createStatement();
				s.setEscapeProcessing(false);

		*/		
			try {
					linkUpdater.setString(1,url);
				System.out.println("Updating url...");
					linksAdded += linkUpdater.executeUpdate();
				System.out.println("Updated url...");
				}
				catch(SQLException e)
				{
					/*this can also result due to a url being appended second 
					 *time. Condition to be taken care of in future version of
					 *this software.
					 *But if this resulted from some connection malfunctioning
					 *then the linkIdRetriever operation would not return 
					 *anything and we can skip the remaining operation for this
					 *particular link.
					 *
					 *TODO: Handle the other cases.
					 */
					 if (e.getErrorCode()==00001)
					 {
					 	//oracle primary key voilation error code
					 	// keep quiet ;)
					 }
					 else
					 	e.printStackTrace();					 
				}
			
				linkIdRetriever.setString(1,url);
				System.out.println("Retrieving Link id...");
				ResultSet rslir = linkIdRetriever.executeQuery();
				String rid="";
				if (rslir.next())
				{
					rid=rslir.getString(1);
				}
				if (rid==null || rid=="" )
				{
					/*skip the remaining process for this particular link
					 *and move on to other records.
					 */
					continue ;
				}
				System.out.println("Link id Retrieved...");
				StringTokenizer stkzr = new StringTokenizer(words," ");
				String v;
				while(stkzr.hasMoreTokens())
				{
					v = stkzr.nextToken().toUpperCase();
					int hid = v.hashCode();
					try
					{
						wordUpdater.setInt(1,hid);
						wordUpdater.setString(2,v);
						System.out.println("Updating word..."+v);
						wordsAdded += wordUpdater.executeUpdate();
						System.out.println("Updated word..."+v);
					}
					catch(SQLException e ) 
					{ 
						/*This can also result if a word is being added twice.
						 *TODO: Handle the other cases!
						 */
						 if (e.getErrorCode()==00001)
						 {
						 	//oracle primary key voilation error code
						 	// keep quiet ;)
						 }
						 else
						 	e.printStackTrace();
					}
					
					try
					{
						wordLinkMapper.setInt(1,hid);
						wordLinkMapper.setString(2,dFragCode + rid);
						System.out.println("Updating word link map...");
						wordLinkMappingAdded += wordLinkMapper.executeUpdate();
						System.out.println("Updated word link map...");
					}
					catch(SQLException e)
					{
						/*The exception above would provide the hint if a link
						 *is being updated for a word that doesnt exist.
						 *Because wordid of wordlinkmapper table is foreign key
						 *and references wordid of wordbank. 
						 *Really hate to do all this.
						 *TODO: Reconsider entire Code in this class!
						 */
						 if (e.getErrorCode()==00001)
						 {
						 	//oracle primary key voilation error code
						 	// keep quiet ;)
						 }
						 else
						 	e.printStackTrace();
					}
				}
/*				
			try {
					bw.write(url,0,url.length());
					bw.newLine();
					bw.write(words,0,words.length());
					bw.newLine();
				}
				catch(IOException e) {e.printStackTrace();}
				try {
					StringTokenizer st = new StringTokenizer(words);
					String q1; 
					while (st.hasMoreTokens())
					{
						q1 = "insert into wordbank(word) values('"+st.nextToken()+"')";
						s.executeUpdate(q1);
					}
				String q1 = 
				}
				String query = "insert into linksbank(link,words) values('"+url+"','"+words+"')";
				try {
					rows+=s.executeUpdate(query);
				}
				catch(SQLException sq ) { sq.printStackTrace();}				
*/
			}
		}
		} catch(SQLException sqe) { sqe.printStackTrace();}
		
		
		System.out.println("Links Added = "+linksAdded);
		System.out.println("Words Added = "+wordsAdded);
		System.out.println("Word <-> Link mappings Added = "+wordLinkMappingAdded);
		
		return linksAdded;
	}
}
