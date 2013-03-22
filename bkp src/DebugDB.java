package net.engine.crawler;


import java.sql.*;
import java.util.*;
import java.io.*;

public class DebugDB {
	
	Connection con ;
	PreparedStatement ps;
	
	BufferedWriter bw ;
	
	
	
	/**
	 * Method DebugDB
	 */
	public DebugDB() {
		try {
		Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		con = DriverManager.getConnection(
			"jdbc:odbc:project");
		ps = con.prepareStatement("insert into linksbank(link,words) "
		+ " values('?','?')");
		
		
		bw = new BufferedWriter(new FileWriter("words.txt"));
		
		}
		catch(Exception e)
		{
			 e.printStackTrace();
		}		
	}	
	
	
	public int update(HashMap map)
	{
		int rows=0;	
		try {		
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
*/			
				Statement s = con.createStatement();
		/*		s.setEscapeProcessing(false);

		*/		try {

				
				bw.write(url,0,url.length());
				bw.newLine();
				bw.write(words,0,words.length());
				bw.newLine();
				}
				catch(IOException e) {e.printStackTrace();}
/*				try {
					StringTokenizer st = new StringTokenizer(words);
					String q1; 
					while (st.hasMoreTokens())
					{
						q1 = "insert into wordbank(word) values('"+st.nextToken()+"')";
						s.executeUpdate(q1);
					}
				String q1 = 
				}
*/				String query = "insert into linksbank(link,words) values('"+url+"','"+words+"')";
				try {
					rows+=s.executeUpdate(query);
				}
				catch(SQLException sq ) { sq.printStackTrace();}
				
			}			
		}
		}
		catch(SQLException sqe)
		{ sqe.printStackTrace();}
		return rows;
	}
}
