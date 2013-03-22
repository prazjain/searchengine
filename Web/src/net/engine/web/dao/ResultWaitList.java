package net.engine.web.dao;


import java.util.*;

public class ResultWaitList {
	private Vector objects;
	private boolean resultsAvailable=false;
	
	public ResultWaitList ()
	{
		objects = new Vector();
	}
	
	public void setResultsAvailable(boolean val)
	{
		resultsAvailable = val;
	}
	
	public boolean isResultsAvailable()
	{
		return resultsAvailable;
	}
	public void addSearchRequest(HashMap rcache)
	{
		objects.add(rcache);
	}
/*
	public void removeThread(Thread enduserThread)
	{
		objects.remove(enduserThread);
	}
*/
	public int getSize()
	{
		return objects.size();
	}
	
	public ArrayList getQueryParams(String fragid)
	{
		/*return arraylist from 'fragid' entry of any of the 
		 *resultsCache objects.
		 */
		 HashMap hmap = (HashMap)objects.get(0);
		 return (ArrayList)hmap.get(fragid);
	}
	public void setResults(String dFragId, ArrayList result)
	{
		int size = objects.size();
		for ( int i =0 ; i < size; i++)
		{
			HashMap hmap = (HashMap)objects.get(i);
			/*overwrite the query params of all cache objects with
			 *the results obtained against them
			 */
			synchronized(hmap)
			{
				hmap.put(dFragId,result);
				int j = Integer.parseInt((String)hmap.get("ResultsPending"));
				hmap.put("ResultsPending",String.valueOf(j-1));
				// notify all waiting on this hmap object
				hmap.notifyAll();
				//notifyAll();
			}
			
		}
	}
	
}
