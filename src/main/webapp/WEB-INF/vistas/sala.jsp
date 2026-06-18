<%@ page contentType="text/html;charset=UTF-8"%>
<%
if (session.getAttribute("nombre") == null) {
	response.sendRedirect(request.getContextPath() + "/index.jsp");
	return;
}
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Sala de espera — Vientos sin fronteras</title>
<link
	href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600&family=Space+Mono:wght@400;700&display=swap"
	rel="stylesheet">
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/css/sala.css">
</head>
<body>
	<a href="<%=request.getContextPath()%>/LogoutServlet"
		class="btn-logout">Salir</a>
	<div class="logo">COPA ROBÓTICA · EDUCABOT 2026</div>
	<h1>⚡ Sala de espera</h1>

	<p class="subtitulo" id="subtitulo">Esperando que se conecten todas las jugadoras...</p>
	<div class="jugadoras-grid" id="grid"></div>
	<div class="contador">
		Conectadas: <span id="contador">0</span> / 5
	</div>

	<!-- Sección de elección de rol -->
	<div class="roles-section" id="roles-section">
		<h2 class="roles-titulo">Elegí tu rol</h2>
		<p class="roles-subtitulo">Cada chica elige un rol distinto. Si tu rol está tomado, esperá que se libere o coordinen entre ustedes.</p>

		<div class="roles-grid">
			<div class="rol-card" data-rol="panel" data-color="panel">
				<div class="rol-icono">🎛️</div>
				<div class="rol-nombre">Panel de Control</div>
				<div class="rol-descripcion">Coordinás al equipo. Ves viento, transformador, energía y alertas.</div>
				<div class="rol-elegido-por" id="rol-tomado-panel"></div>
				<button class="btn-elegir-rol" onclick="elegirRol('panel')">Elegir este rol</button>
			</div>

			<div class="rol-card" data-rol="aero" data-color="aero">
				<div class="rol-icono">💨</div>
				<div class="rol-nombre">Aerogenerador</div>
				<div class="rol-descripcion">Entrenás el modelo ML y controlás la orientación con gestos.</div>
				<div class="rol-elegido-por" id="rol-tomado-aero"></div>
				<button class="btn-elegir-rol" onclick="elegirRol('aero')">Elegir este rol</button>
			</div>

			<div class="rol-card" data-rol="tecnico" data-color="tecnico">
				<div class="rol-icono">🔧</div>
				<div class="rol-nombre">Técnico Especialista</div>
				<div class="rol-descripcion">Buscás datos técnicos y decodificás códigos de alerta.</div>
				<div class="rol-elegido-por" id="rol-tomado-tecnico"></div>
				<button class="btn-elegir-rol" onclick="elegirRol('tecnico')">Elegir este rol</button>
			</div>

			<div class="rol-card" data-rol="prog" data-color="prog">
				<div class="rol-icono">⚙️</div>
				<div class="rol-nombre">Programador</div>
				<div class="rol-descripcion">Configurás transformador, subestación y aerogenerador con bloques.</div>
				<div class="rol-elegido-por" id="rol-tomado-prog"></div>
				<button class="btn-elegir-rol" onclick="elegirRol('prog')">Elegir este rol</button>
			</div>

			<div class="rol-card" data-rol="dist" data-color="dist">
				<div class="rol-icono">⚡</div>
				<div class="rol-nombre">Distribuidor de Energía</div>
				<div class="rol-descripcion">Distribuís la energía a las 4 zonas e iluminás la ciudad.</div>
				<div class="rol-elegido-por" id="rol-tomado-dist"></div>
				<button class="btn-elegir-rol" onclick="elegirRol('dist')">Elegir este rol</button>
			</div>
		</div>

		<div class="acciones-finales">
			<button class="btn-jugar" id="btnJugar" onclick="window.location.href='<%= request.getContextPath() %>/JuegoServlet'">
				⚡ Jugar mi rol
			</button>
		</div>
	</div>

<script>
  const miNombre = '<%= session.getAttribute("nombre") %>';
  const contextPath = '<%= request.getContextPath() %>';
</script>
	<script src="<%=request.getContextPath()%>/js/sala.js"></script>
</body>
</html>
