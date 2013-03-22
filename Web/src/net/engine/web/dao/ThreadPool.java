package net.engine.web.dao;


import java.util.*;
import java.io.*;

import java.sql.*;

public class ThreadPool {

  private ArrayList pool;
  Connection dbCon;

  public ThreadPool(Connection con,DatabaseFragment dFrag,int poolSize,int queryType) throws SQLException
  {
    pool = new ArrayList(poolSize);
    this.dbCon = con ;
    for ( int count =0 ; count < poolSize; count ++ ) 
    {
    	WorkerThread t = new WorkerThread(dbCon,dFrag,queryType);
      	pool.add(t);
      	t.setDaemon(true);	// suitable for now?
      	t.start();
    }
  }
  
  public void destroy()
  {
  	int poolSize = pool.size();
  	for ( int count =0 ; count < poolSize; count ++ ) 
    {
    	WorkerThread t = (WorkerThread)pool.get(count);
      	t.destroy();
    }
  }
/*
  public WorkerThread get()
  {
	 int size = pool.size();
     for (int i =0 ; i < size ; i++)
     {
         WorkerThread wt= (WorkerThread) pool.get(i);
         synchronized (wt) 
         {
             if (wt.isFree()) 
             {
                wt.setFree(false);
                return wt;
             }
         }
     }
     return null;
  }

  public boolean  put (WorkerThread wt) {
    return pool.add(wt);
  }
*/
}