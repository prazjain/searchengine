package net.engine.crawler;

/**
 *This class would parse these attributes
 *A - href
 *BASE - href
 *FRAME - src
 *META - name="keywords" content="all content"
 *META - name="description" content="all content" 
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.util.regex.*;

import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*; 


import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;

//import org.apache.log4j.FileAppender;
//import org.apache.log4j.SimpleLayout;


 
 public class SpiderWorker implements Runnable
 {

	protected ISpiderReportable spider ;
	protected HashSet _internalLinks;
	protected HashSet _externalLinks;
	protected String words;
		 	
 	protected HTMLEditorKit.Parser parser = new ParserDelegator();
 	//protected HTMLEditorKit.ParserCallback  parserCallback ;
 	protected TagParserCallback  parserCallback ;
 
 // STILL NEED TO FIND A WAY TO GET BOTH THESE VARIABLE VALUES	 	
 //	protected String currentBaseURL="http://java.sun.com"; // will have string in the format starting with "http[s]://" and with no "/" in the end.
 //	protected String currentURL=currentBaseURL; // format = starts with "http[s]://" and with no "/" in the end.
 	
 	protected String currentBaseURL="";
 	protected String currentURL="";
 	
 	protected String name;
 	
 	protected static final Logger logger = Logger.getLogger(SpiderWorker.class);
 	
 	public SpiderWorker(ISpiderReportable spider,String name)
 	{
 		this.spider=spider;
 		this.parserCallback = new TagParserCallback(this);
 		this.name=name;
 	}
 	
 	public String getName() { return name; }
 	
 	public void setWords(String w) { words=w; }
 	
 	public HashSet getFilteredWords() { return spider.getFilteredWords(); } 	
 	
 	public void categorizeLink(String href)
 	{
 		
 		if (href== null || "".equals(href)) return ;
 		
 		if (href.charAt(href.length()-1)=='/')
 		{
 			/*eg: http://java.sun.com/logos/
 			 * remove the trailing "/"
 			 */ 			
 			href=href.substring(0,href.length()-1);
 		}
 		
 		int pageAnchorIdx = href.indexOf("#");
 		
 		if(pageAnchorIdx != -1)
 		{
 		/*Links with 
 		 *<scheme>://<authority><path>?<query>#<fragment>
 		 *everything same except for fragment should not be entered into the system.
 		 */
 			href = href.substring(0,pageAnchorIdx);
 		}
 		
 		
 		if (href.startsWith("http://") || href.startsWith("https://"))
 		{
 			// if name is same as existing site then 
 			if (href.equals(currentBaseURL))
 			{
 				_internalLinks.add(href); 				
 				logger.log(Level.INFO, "Internal Link :" + href);
 			}
 			// can be link to external site.
 			else 
 			{
 				_externalLinks.add(href);
 				logger.log(Level.INFO, "External Link :" + href); 				
 			}
  		}
 		else if (href.startsWith("/"))
 		{
 			/*starts with root of current domain.
 			 *currentBaseURL = http://myhomepage.yahoo.com/somedir/one.html
 			 *href = /two.html
 			 *link generated = http://myhomepage.yahoo.com/two.html
 			 */
 			
 			_internalLinks.add(currentBaseURL+href);
			//logger.log(Level.INFO, "starts With '/' :" + href + "  - "+currentBaseURL +href); 			
			logger.log(Level.INFO, currentBaseURL +href); 			
 		}
 		else
 		{
 			/*href would start with a '..' or '.' or just some file name
 			 *href = "../two.html" 
 			 *href = "./two.html"
 			 *href = "two.html" 
 			 *references would be resolved according to current directory.
 			 */
 		
 		
 			if (href.startsWith(".."))
 			{
 				/*path is with respect to current dir.
 				 *currentURL = http://myhomepage.yahoo.com/somedir/one.html
 				 *href = ../two.html
 				 *link generated = http://myhomepage.yahoo.com/two.html
 				 */
 			 
 				// get upto parent directory and strip the remaining URL.
 				String tmp = currentURL.substring(0,currentURL.lastIndexOf("/"));
 				tmp = tmp.substring(0,tmp.lastIndexOf("/"));
 				_internalLinks.add(tmp + href.substring(2));
 				//logger.log(Level.INFO, "starts With '..' " + href + "  - "+currentURL + href.substring(1)); 			
 				logger.log(Level.INFO, tmp + href.substring(2)); 			
 			}
 			else if (href.startsWith("."))
 			{
 				/*path is with respect to current dir.
 				 *currentURL = http://myhomepage.yahoo.com/somedir/one.html
 				 *href = ./two.html
 				 *link generated = http://myhomepage.yahoo.com/somedir/two.html
 				 */
 			 
 				_internalLinks.add(currentURL.substring(0,currentURL.lastIndexOf("/")) + href.substring(1));
 				//logger.log(Level.INFO, "starts With '.' or curr dir :" + href + "  - "+currentURL.substring(0,currentURL.lastIndexOf("/")) + href.substring(1)); 			
 				logger.log(Level.INFO, currentURL.substring(0,currentURL.lastIndexOf("/")) + href.substring(1)); 			
 			}
 			else
 			{
 				_internalLinks.add(currentURL + "/" + href);
 				logger.log(Level.INFO,currentURL + "/" + href);
 					
/* 				int lastIndex = currentURL.lastIndexOf("/");
 				if (lastIndex!=6 || lastIndex!=7)
 				{
 					//it means that currentURL is not like this "http[s]://www.yahoo.com"
 					_internalLinks.add(currentURL.substring(0,currentURL.lastIndexOf("/")) + "/" + href);
 					logger.log(Level.INFO, currentURL.substring(0,currentURL.lastIndexOf("/")) + "/" + href);
 				}
 				else
 				{
 					//its like "http[s]://www.yahoo.com"
 					_internalLinks.add(currentURL + "/" + href);
 					logger.log(Level.INFO,currentURL + "/" + href);
 				}
 */				
 			}
 		}
 	}
	
 	public void run()
 	{
 		try
 		{
 			while(true)
 			{
 				while((currentURL=spider.getUnparsedLink())==null)
 				{
 					Thread.currentThread().sleep(1500);
 				}
 				
 				setUpEnvironment();
 				
 				parseHTML();
 				
 				callbackSpider();

 				tearDownEnvironment();
 			} 			
 		}
 		catch(InterruptedException ie) 
 		{
 			/*Assuming that this thread would be interrupted only when we want 
 			 *to shut it down.
	 		 */
	 		 System.out.println("Worker Interrupted :"+name);
			// ie.printStackTrace();	
 		}
 	}
	
 	public void parseHTML()
 	{
		try
		{
			 		 				 				
			URLConnection urc = new URL (currentURL).openConnection();
			InputStreamReader isr = new InputStreamReader(urc.getInputStream());

			parser.parse(isr,parserCallback,true);	
		}
 		catch (MalformedURLException mue){
 			logger.log(Level.INFO,"Malformed URL :" + currentURL);
 		}
 		catch(IOException e)
 		{
 			logger.log(Level.DEBUG,e.getMessage());
 			//e.printStackTrace();
 		} 
 	}

/*
 	public void run()
 	{
 		try
 		{
 			while((currentURL=spider.getUnparsedLink())==null)
 			{
 				Thread.currentThread().sleep(1500);
 			}
 			
 			setUpEnvironment();
 			
 			parseHTML();
 			
 			callbackSpider();
			tearDownEnvironment();
 		}
 		catch(InterruptedException ie) 
 		{
 			/*Assuming that this thread would be interrupted only when we want 
 			 *to shut it down.
	 		 */
/*	 		 
			 ie.printStackTrace();	
			 logger.log(Level.DEBUG, "Spider Worker thread shut down");
 		}
 	}
 	

 	public void parseHTML()
 	{
		try
		{ 		 				 				
		FileInputStream fis = new FileInputStream(new File("../java.sun.com.txt"));
		InputStreamReader isr = new InputStreamReader(fis);

			parser.parse(isr,parserCallback,true);	
		}
 		catch(IOException e)
 		{
 			e.printStackTrace();
 		} 
 	}

*/
 	 	
 	public void callbackSpider()
 	{
		spider.addInternalLinks(_internalLinks);
		spider.addExternalLinks(_externalLinks);
		spider.updateParsedLinks(currentURL,words);
 	}
 	
 	public void setUpEnvironment()
 	{
 		_internalLinks = new HashSet();
		_externalLinks = new HashSet();
		//words = new StringBuffer();
		
		if (currentURL.charAt(currentURL.length()-1)=='/')
		{
			/*eg: http://java.sun.com/logos/
 			 * remove the trailing "/"
 			 */ 			
 			currentURL=currentURL.substring(0,currentURL.length()-1);
 			//logger.log(Level.DEBUG, "currentURL changed to (removed trailing '/') :"+ currentURL);
 		}
 		
 		int idx = currentURL.indexOf("//"); // for ["http://" : idx=5], ["https://" : idx=6]
 		
 		int index = currentURL.indexOf("/",idx + 2); // index for 3rd "/" in URL.
 		if (index != -1)
 		{
 			/*This means that a third "/" exists in URL string
 			 *now extract the base url from this.
 			 */
 			currentBaseURL = currentURL.substring(0,index);
		}
		else
		{
			currentBaseURL = currentURL;
		}
		
		/*setUpEnvironment for tagParserCallback */
		parserCallback.setUpEnvironment();
 	}
 	
 	public void tearDownEnvironment()
 	{
 		_internalLinks = null;
 		_externalLinks = null;
 		//words = null;
 		currentBaseURL = null;
 		/*tearDownEnvironment for tagParserCallback*/
 		parserCallback.tearDownEnvironment();
 	}
 	
 }
 
 
 class TagParserCallback extends HTMLEditorKit.ParserCallback
 {
 	private SpiderWorker worker;
 	
 	/*Alpha numeric pattern string*/
 	private Pattern pattern = Pattern.compile("[a-zA-Z0-9]{3,}");
 	
 	/*These are info's that are specific to a particular page being parsed.
 	 *These should be setup and teardown before and after a page is parsed.
 	 */
 	private StringBuffer words;
 	private HashSet uniqueWords;
 	private int maxWords = 15;
 	//private int wordCount = 0; 	
 	public TagParserCallback (SpiderWorker sw)
 	{
 		worker = sw;	
 	}
 	public void setUpEnvironment()
 	{
 		words = new StringBuffer();
 		uniqueWords = new HashSet();
 	}
 	public void tearDownEnvironment()
 	{
 		words = null;
 		//uniqueWords.removeAll();
 		uniqueWords = null;
 	}
 	public void handleStartTag(HTML.Tag t, MutableAttributeSet a , int pos)
 		{
 			try {
 			
 			if (t==HTML.Tag.A)
 			{
 				String hrefValue = (String)a.getAttribute(HTML.Attribute.HREF);
 				//logger.log(Level.INFO,t + " " + hrefValue);
 				worker.categorizeLink(hrefValue);
 			}
 			else
 			{
 			//	logger.log(Level.DEBUG,"Start :"+t  );
 			}
 			}
 			catch(Exception e){ e.printStackTrace();	}
 		}
 		
 		public void handleSimpleTag(HTML.Tag t , MutableAttributeSet a,int pos)
 		{
 			try
 			{
 			if (t== HTML.Tag.BASE )
 			{
 				String hrefValue = (String)a.getAttribute(HTML.Attribute.HREF);
 				//logger.log(Level.INFO,t + " " + hrefValue);
 				worker.categorizeLink(hrefValue);
 			}
 			else if (t == HTML.Tag.FRAME)
 			{
 				String srcValue= (String)a.getAttribute(HTML.Attribute.SRC);
 				//logger.log(Level.INFO, t +" "+ srcValue); 				
 				worker.categorizeLink(srcValue);
 			}
 			else if (t == HTML.Tag.META)
 			{
 				/*TODO:
 				 *Till now this meta info is not being sent back to Spider !
 				 */
 				String nm = (String)a.getAttribute(HTML.Attribute.NAME);
 				String content = (String)a.getAttribute(HTML.Attribute.CONTENT);
 				if ("keywords".equalsIgnoreCase(nm) || "description".equalsIgnoreCase(nm))
 				{
 				//	logger.log(Level.INFO, t + " " + nm + " = " + content);
 					//logger.log(Level.INFO, t + " --- " + a);
 				}
 			}
 			}
 			catch(Exception e){ e.printStackTrace();	}
 		}
 		
 		public void handleText(char[] text, int pos)
 		{
 			// we are looking for words deep into the doc!
 			if (pos < 150) return;

 			if (uniqueWords.size() >= maxWords)
 			{
 				/*we have already captured the words for this page
 				 *skip the rest of the words
 				 */
 				 return;
 			}
 			
 			// tokenizer with default whitespace delimiters.
 			StringTokenizer tokenizer  = new StringTokenizer(new String(text));
 			
 			HashSet filteredWords = worker.getFilteredWords();
 			
 			while (tokenizer.hasMoreTokens())
 			{
 				String token = tokenizer.nextToken().toUpperCase();
 				if ((!filteredWords.contains(token)) 
 				&& (pattern.matcher(token).matches()))
 				{
 					uniqueWords.add(token);
 					if (uniqueWords.size() >= maxWords)
 					{
 						Iterator itr = uniqueWords.iterator();
 						while (itr.hasNext())
 						{
 							words.append((String)itr.next());
 							words.append(" ");
 						}
						worker.setWords(words.toString());
						return;
 					}
 					
 				}
 			}
 			
 		}
 }