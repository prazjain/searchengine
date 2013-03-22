package net.engine.web.dao;

import java.util.*;
import java.sql.*;

public class BatchWork extends Work
{
	public BatchWork(String st,ArrayList batchWork)
	{
		searchTerm = st;
		this.data = batchWork;
		results = new ArrayList();
	}
	
	public void perform(Statement lookup)
	{
		/*This has the logic to execute batch rowid lookup queries.
		 */
		 try
		 {
		 	int size = data.size();
		 	for ( int i =0; i < size ; i++)
		 	{
		 		String qry = "select link from linkbank where rowid='"+data.get(i)+"'";
				ResultSet rs = lookup.executeQuery(qry);
				if(rs.next())
				{
					//We will get one record only per rowid.
					// will always enter here
					results.add(rs.getString("link"));
				}
				else
				{
					System.out.println("Requested row was not present(deleted)!");
				}
		 	}
		 		 	
		 }
		 catch(SQLException sqe) { sqe.printStackTrace(); }
		 
	}
	
	
}
