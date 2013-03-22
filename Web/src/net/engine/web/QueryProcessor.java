package net.engine.web;

import java.util.*;
import javax.servlet.ServletConfig;
import net.engine.web.dao.*;

/**
 *Query Processor has the main task of interpreting the query which have
 *not been implemented in this version of this software. It would do task 
 *such as doing boolean AND , OR stuff on query. Supporting caching 
 *facility for the query repeated often. Other things that would include
 *searching in specific documents such as pdf,doc,rtf,ppt,html etc.
 *Currently QueryProcessor just fires query and gets results from DAO.
 */

public class QueryProcessor {
	
	private DAO dao;
	private ArrayList queryCacheIndex;
	private ArrayList cache;
	private int cacheSize = 5;
		
	public QueryProcessor(ServletConfig config) throws ClassNotFoundException
	{
		dao = new DAO(config);
		queryCacheIndex = new ArrayList(cacheSize);
		cache = new ArrayList(cacheSize);
	}

	/**
	 * Method process
	 * @param qry
	 */
	public ArrayList process(String qry) 
	{
		ArrayList result;
		//Convert the string to upper case before searching
		qry = qry.trim().toUpperCase();
		synchronized (queryCacheIndex)
		{
			int idx = queryCacheIndex.indexOf(qry);
			if (idx >= 0 )
			{
				result = (ArrayList) cache.get(idx);
				// append the recently fired query to top of stack.
				queryCacheIndex.remove(idx);
				cache.remove(idx);
				queryCacheIndex.add(qry);
				cache.add(result);
				return result;
			}
		}
		
		//else
		//{
			result = dao.query(qry);
			if (result.size() == 0 ) return result;
			
			synchronized (queryCacheIndex)
			{
				if (queryCacheIndex.size() < cacheSize - 1 )
				{
					queryCacheIndex.add(qry);
					cache.add(result);
				}
				else
				{
					queryCacheIndex.remove(0);
					cache.remove(0);
					queryCacheIndex.add(qry);
					cache.add(result);
				}
			}
		//}
		return result;
	}
}
