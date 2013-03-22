
<%@ page language="java"%>
<%@ page import="java.util.ArrayList"%>

<html>
<head><title>Search Results</title></head>

<body><br>
<% 
ArrayList r = (ArrayList)request.getSession().getAttribute("result");
int size = r.size();
%>
Time taken to process query: <%=request.getSession().getAttribute("time")%> millisec.
<%
		if ( size == 0 )
		{
%>
			<b> No match found for the search criteria </b>
	
<%		}	else
		{
%>
		    <p><b> Search Results : </b><br>

			<ol>
<%
			for ( int i =0 ; i < size;i++)
			{
%>
				<li><a href='<%=r.get(i)%>'> <%=r.get(i)%> </a><br>
<%
			}
		}
		
		request.getSession().invalidate();
		
%>
		</ol><br></body></html>
		
		