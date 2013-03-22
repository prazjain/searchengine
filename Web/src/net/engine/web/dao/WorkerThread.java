package net.engine.web.dao;

import java.sql.*;

public class WorkerThread extends Thread
{
	private boolean free=true;
	private boolean keepLooping = true;
	
	//private PreparedStatement lookup;
	private Statement lookup;
	
	private DatabaseFragment dFrag;
	private int queryType;
	
	public WorkerThread(Connection con, DatabaseFragment dFrag,int qt) throws SQLException
	{
		this.dFrag  = dFrag;
		this.queryType = qt;
		//lookup = con.prepareStatement(query);
		lookup = con.createStatement();
	}
	
	/**
	 *sending word look up to every fragment is not appropriate
	 *because a word would exist in only one table fragment
	 */
	 /*
	private ResultSet execute()
	{
		ResultSet rs = lookup.executeQuery();
		if (rs.next())
		{
			// so the word exists!
			wordLinkLookup() //TODO: 
			return;
		}
		return null;
	}
	*/
/*	
	public boolean isFree()
	{
		return free;
	}
	public void setFree(boolean val)
	{
		free = val;
	}
*/	
	
	public void run()
	{
		// add a procedure to gracefully shut down the thread here
		try
		{
			while (keepLooping)
			{
				Work wrk = dFrag.getWork(queryType);//,lookup);
				wrk.perform(lookup);
				// BEWARE of null being returned from EXECUTE method!!
				if (keepLooping)
					dFrag.submitResult(wrk.getSearchTerm(),queryType,wrk.getResults());
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public void destroy()
	{
		keepLooping = false;
		try 
		{
			lookup.close();			
		} catch(SQLException e ) { e.printStackTrace() ;}

	}
}
