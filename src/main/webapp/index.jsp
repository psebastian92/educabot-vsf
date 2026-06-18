<%@ page contentType="text/html;charset=UTF-8"%>
<%
if (session.getAttribute("nombre") != null) {
	request.getRequestDispatcher("/WEB-INF/vistas/sala.jsp").forward(request, response);
	return;
}
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Vientos sin fronteras</title>
<link
	href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600&family=Space+Mono:wght@400;700&display=swap"
	rel="stylesheet">
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/index.css">
</head>
<body>
	<div class="login-wrapper">
		<div class="logo">COPA ROBÓTICA · EDUCABOT 2026</div>
		<h1>⚡ Vientos sin fronteras</h1>
		<p class="subtitulo">Ingresá para unirte a la sala de espera</p>

		<%
		if (request.getAttribute("error") != null) {
		%>
		<div class="error-msg"><%=request.getAttribute("error")%></div>
		<%
		}
		%>

		<form method="post" action="LoginServlet" class="login-form">
			<div class="field">
				<label>Jugadora</label> <select name="nombre">
					<option value="Alma">Alma</option>
					<option value="Catalina">Catalina</option>
					<option value="Daira">Daira</option>
					<option value="Isabella">Isabella</option>
					<option value="Kiara">Kiara</option>
				</select>
			</div>
			<div class="field">
				<label>Clave</label> <input type="password" name="clave"
					placeholder="••••••••••">
			</div>
			<button type="submit" class="btn-ingresar">Ingresar →</button>
			<a href="<%= request.getContextPath() %>/manual.html" 
   class="btn-manual" target="_blank">
  📋 Ver manual antes de jugar
</a>
		</form>
	</div>
</body>
</html>