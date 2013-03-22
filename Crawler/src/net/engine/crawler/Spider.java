
package net.engine.crawler;

import java.net.*;
import java.io.*;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;


public class Spider implements ISpiderReportable
{

	/*This is the set of internal links that this spider found when crawling this site.
	 *It holds [ absolute url ]
	 */
	private HashSet internalLinks=new HashSet();
	/*This is a set that holds all the external links that were found by this spider.
	 *It holds [ absolute url ]
	 *eg: it can also be a url that isnt home page of a site
	 * http://java.sun.com/download/index.jsp?download=jdk1.5
	 */
	private HashSet externalLinks= new HashSet();
	
	/*This is the set of internal links that this spider crawled with the help of its workers.
	 *It holds a mapping of [ absolute url, words ]
	 */
	private HashMap parsedLinks= new HashMap();
	
	/*These links are granted to the spider on request for new links by Database components.
	 */
	private LinkedList freshLinks = new LinkedList();
	
	/*It stores the words set that are filtered by the spiderworker
	 *And searchTerms of length=1 would be by default filtered.
	 */
	private HashSet filteredWords = new HashSet();
	
	private static final int POOL_SIZE = 10;
	private SpiderWorker pool[] = new SpiderWorker[POOL_SIZE];
	private ThreadGroup workerGroup = new ThreadGroup("SpiderWorker");
	
	protected static final Logger logger = Logger.getLogger(Spider.class);

/*
	static
	{
		PropertyConfigurator.configure(".."+File.separator+"loggerConfig.properties");		
	}
*/
	public Spider()
	{
		 		
 		/*Configure log4j only once .. some people configure it in each and every class
 		 *this is plainly wrong and inefficient.
 		 *
 		 *PS : Replace this code to better place.
 		 */
// 		 PropertyConfigurator.configure(".."+File.separator+"loggerConfig.properties");
 		 //PropertyConfigurator.configure(".."+File.separator+".."+File.separator+"loggerConfig.properties");
 		 
 		 for (int i =0;i<POOL_SIZE;i++)
 		 {
 		 	pool[i] = new SpiderWorker(this,"SpiderWorker "+i);
 		 	Thread th = new Thread(workerGroup,pool[i],pool[i].getName());
 		 	th.setDaemon(true);
 		 	th.start();
 		 }
 		 
 		 try {
 		 BufferedReader br = new BufferedReader(new FileReader(".." + File.separator+"FilterWords.txt"));
 		 String word ;
 		 while((word=br.readLine())!=null)
 		 {
 		 	filteredWords.add(word);
 		 }
 		 }
 		 catch(IOException e)
 		 { e.printStackTrace();	 }
 		 
	}
	
	//----------------
	// implementation of ISpiderReportable Interface
	
	/**
	 *This would first try to give a link from the set of links that have just been found 
	 *by the spider's workers, if these links have exhausted then a link would be alloted 
	 *from freshLinks collections. If both have exhausted then null is returned.
	 */
	public String getUnparsedLink()
	{
		// first give the links from internalLinks cache if links available.
		synchronized(internalLinks)
		{
			if (internalLinks.size() > 0)
			{
				Iterator itr = internalLinks.iterator();
				String sb = (String)itr.next();
				internalLinks.remove(sb);
				
//				logger.log(Level.DEBUG, "Assigning an unparsed internal link to SpiderWorker :"+sb);
				
				return sb;
			}
		}
		
		// else give a link from unparsedLinks cache.
		synchronized (freshLinks)
		{
			if (freshLinks.size() > 0)
			{
//				String s = (String)freshLinks.removeFirst();
//				logger.log(Level.DEBUG, "Assigning an unparsed fresh link to SpiderWorker :"+s);
//				return s;
				return (String) freshLinks.removeFirst();		
			}
		}
		
		// if no link is available then return null.
		return null;
	}
		
	public void addInternalLinks(HashSet links)
	{
		synchronized(internalLinks)
		{
			Iterator itr = links.iterator();
			while (itr.hasNext())
			{
				Object o = itr.next();
				if (!parsedLinks.containsKey(o))
				{
					// if not parsed earlier then add to internal links set.
					internalLinks.add(o);							
				}
			}
		}
	}

	/**
	 *These external links need to be updated back into the database.
	 */	
	public void addExternalLinks(HashSet links)
	{
		synchronized(externalLinks)
		{
			externalLinks.addAll(links);	
		}		
	}

	/**
	 *These need to be updated back into the database.
 	 */
	public void updateParsedLinks(String link,String words)
	{
		synchronized (parsedLinks)
		{
			if (!parsedLinks.containsKey(link))
				parsedLinks.put(link,words);
			logger.log(Level.INFO,"updated parsed link");
		}		
	}
	
	public HashSet getFilteredWords()
	{
		return filteredWords;
	}
	
	//----------------
	
	/**
	 *When fresh links have been downloaded add them to freshLinks Collection.
	 */	
	public void updateFreshLinks(LinkedList ll)
	{
		synchronized (freshLinks)
		{
			freshLinks.addAll(ll);
			notifyAll();
		}
	}
	
	
	public static void main(String args[])
	{
		try
		{
			System.setProperty("http.proxySet","true");
			System.setProperty("http.proxyHost","172.18.10.1");
			System.setProperty("http.proxyPort","3128");

			PropertyConfigurator.configure(".."+File.separator+"loggerConfig.properties");


/// START DEBUGGING CODE

			DebugDB d = new DebugDB();
					
//// END DEBUGGING CODE


			Authenticator.setDefault(new MyAuthenticator("prashantj_astjan05","prashantj_astjan05"));

			Spider s = new Spider();
			//SpiderWorker sp  = new SpiderWorker(s);

			s.freshLinks.add("http://java.sun.com/");
			//s.freshLinks.add("http://www.yahoo.com");
			
			
			InputStreamReader i = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(i);
			String cmd="";
			while (true)
			{
				System.out.print("\n\nSpIdErCoNsOlE >");
				cmd=br.readLine();
				if ("help".equals(cmd))
				{
					System.out.println("********************************************");
					System.out.println("close : Shuts down the application. Interrupts Worker Threads");
					System.out.println("stats : Show the size of Internal Data Stores of Links");
					System.out.println("update: This would update the internal / external links into database."
					 +"Available only during debugging phase.");
					System.out.println("********************************************");
				}
				if ("update".equals(cmd))
				{
					System.out.println("********************************************");
					
					HashMap plinks = s.parsedLinks;
					s.parsedLinks = new HashMap();
					int rows = d.update(plinks);
					System.out.println("Updated "+rows+"/"+plinks.size()+ " into database ");
					//plinks.removeAll();
					plinks=null;
					
					System.out.println("********************************************");
					
				}
				if ("close".equals(cmd))
				{
					// close the application
				//	s.workerGroup.interrupt();
					System.out.println("********************************************");
					System.out.println("			SYSTEM SHUT DOWN				");
					System.out.println("********************************************");
					
					// just break out of while LOOP
					break;
					
				/*	Thread grp [] = new Thread[POOL_SIZE];
					s.workerGroup.enumerate(grp);
					for (int j =0 ;j < POOL_SIZE;j++)
					{
						grp[j].join();
					}
						
						
					System.exit(0);
					
					*/
				}
				else if ("stats".equals(cmd))
				{
					System.out.println("\n\n");
					System.out.println("InternalLinks size="+s.internalLinks.size());
					System.out.println("ExternalLinks size="+s.externalLinks.size());
					System.out.println("ParsedLinks size="+s.parsedLinks.size());
					System.out.println("FreshLinks size="+s.freshLinks.size());
				}
				
			}
			
			

			//new Thread(sp).start();

			//String upass = "prashantj_astjan05:prashantj_astjan05";
			//urc.setRequestProperty("Proxy-Authorization","Basic "+Base64.encode(upass));
			
		
		/*				
			
			FileWriter fw = new FileWriter(new File(".."+File.separator+"website code.txt"));
			BufferedWriter bw = new BufferedWriter(fw);
			
			
			int i=-1;
			System.out.println("---------------------------------");
			while ((i=isr.read())!=-1)
			{
				bw.write(i);
			}
			System.out.println("---------------------------------");
		*/	
			



		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

}



class MyAuthenticator extends Authenticator
{
	private String us,pwd;

	public MyAuthenticator(String u ,String p) { us=u;pwd=p;}
	protected PasswordAuthentication getPasswordAuthentication()
	{
		return new PasswordAuthentication(us,pwd.toCharArray());
	}
}

