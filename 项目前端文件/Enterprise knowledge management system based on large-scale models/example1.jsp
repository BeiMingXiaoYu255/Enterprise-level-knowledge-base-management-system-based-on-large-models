<%@ page contentType="text/html;charset=GB2312"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>求1+2+3+...+100的和</title>
</head>
<body>
<%   //这是Java程序
   int i,sum=0;
   for(i=1;i<=100;i++){  
      sum=sum+i;
   }
%> 
<P>  1到100的连续和是：<%=sum %>

</body>
</html>