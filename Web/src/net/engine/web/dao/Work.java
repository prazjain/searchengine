package net.engine.web.dao;

import java.util.*;
import java.sql.*;

public class Work 
{
	protected String searchTerm;
	protected ArrayList data;
	protected ArrayList results;
	
	public Work() {  }
		
	public Work(String st,String qry)
	{
		searchTerm = st;
		data = new ArrayList();
		data.add(qry);
		results = new ArrayList();
	}
	
	public void perform(Statement lookup)
	{
		/*This has the logic to execute word lookup queries
		 */
		 try
		 {
		 	int val = ((String)data.get(0)).hashCode();
		 	String qry = "select rid from wordlinkmap where wordid="+val;
		 	ResultSet rs = lookup.executeQuery(qry);
			while (rs.next())
			{
				// so the word exists!
				results.add(rs.getString("rid"));
			}
		 }
		 catch(SQLException sqe) { sqe.printStackTrace(); }
	}
	
	public ArrayList getResults()
	{
		return results;
	}
	public String getSearchTerm()
	{
		return searchTerm;
	}
}
