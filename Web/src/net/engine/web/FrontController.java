package net.engine.web;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class FrontController extends HttpServlet {
	
	private QueryProcessor qProcessor;
	
	public void init()
	{
		try 
		{
			qProcessor = new QueryProcessor ( getServletConfig() );
		} catch (ClassNotFoundException cnfe) {cnfe.printStackTrace();}		
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException 
	{
		String searchTerm = req.getParameter("search");
		long arrivalTime = System.currentTimeMillis();
		ArrayList result = qProcessor.process(searchTerm);
		long totalTime = System.currentTimeMillis() - arrivalTime;
		
		req.getSession().setAttribute("result",result);
		req.getSession().setAttribute("time",new Long(totalTime));
		
		RequestDispatcher rd = req.getRequestDispatcher("view.jsp");
		rd.forward(req,res);
		//can implement paging by putting into session.
	
	/*
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		out.println("<html><head><title>Search Results</title></head>");
		out.println("<body><br>");
		int size = result.size();
		if ( size == 0 )
			out.println("<b> No match found for the search criteria </b>");
		else
			out.println("<p><b> Search Results : </b><br>");
		out.println("<ol>");
		for ( int i =0 ; i < size;i++)
		{
			out.println("<li><a href=\""+result.get(i)+"\">"+ result.get(i) +"</a><br>");
		}
		out.println("</ol><br></body></html>");
		out.close();
	*/
		
	}


	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException , IOException 
	{
		doGet(req,res);
	}	
}
