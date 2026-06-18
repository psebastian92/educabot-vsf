<%@ page contentType="text/html;charset=UTF-8" %>
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
  <title>Árbitro — Vientos sin fronteras</title>
  <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= request.getContextPath() %>/css/juego.css">
</head>
<body>

<div class="header">
  <div class="logo">COPA ROBÓTICA · EDUCABOT 2026</div>
</div>

<!-- SELECCIÓN DE ROL -->
<div id="role-screen">
  <div class="role-intro">
    <h1>⚡ Elegí tu rol</h1>
    <p>Hola <strong><%= session.getAttribute("nombre") %></strong> — el árbitro te va a hacer preguntas de tu rol y evalúa tus respuestas.</p>
  </div>
  <div class="roles-grid">
    <div class="role-card" style="border-color:var(--panel)" onclick="selectRole('panel')">
      <div class="role-card-icon">🎛️</div>
      <div class="role-card-name" style="color:var(--panel)">Panel de Control</div>
      <div class="role-card-desc">Coordinás al equipo. Ves viento, transformador, alertas y energía.</div>
    </div>
    <div class="role-card" style="border-color:var(--aero)" onclick="selectRole('aero')">
      <div class="role-card-icon">💨</div>
      <div class="role-card-name" style="color:var(--aero)">Aerogenerador</div>
      <div class="role-card-desc">Entrenás el modelo ML y controlás la orientación con gestos.</div>
    </div>
    <div class="role-card" style="border-color:var(--tecnico)" onclick="selectRole('tecnico')">
      <div class="role-card-icon">🔍</div>
      <div class="role-card-name" style="color:var(--tecnico)">Técnico Especialista</div>
      <div class="role-card-desc">Buscás info técnica y decodificás alertas y modelos.</div>
    </div>
    <div class="role-card" style="border-color:var(--prog)" onclick="selectRole('prog')">
      <div class="role-card-icon">💻</div>
      <div class="role-card-name" style="color:var(--prog)">Programador</div>
      <div class="role-card-desc">Programás transformador, subestación y aerogenerador.</div>
    </div>
    <div class="role-card" style="border-color:var(--dist)" onclick="selectRole('dist')">
      <div class="role-card-icon">🗺️</div>
      <div class="role-card-name" style="color:var(--dist)">Distribuidor de Energía</div>
      <div class="role-card-desc">Distribuís energía a las 4 zonas e iluminás la ciudad.</div>
    </div>
  </div>
</div>

<!-- CHAT -->
<div id="chat-screen">
  <div id="role-banner" class="role-banner"></div>
  <div class="score-panel">
    <span style="color:var(--muted)">Puntaje acumulado</span>
    <span class="score-total" id="score-display">— / —</span>
    <button class="btn-cambiar" onclick="resetRole()">Cambiar rol</button>
  </div>
  <div class="chat-messages" id="messages"></div>
  <div class="chat-input-area">
    <div id="input-area" style="display:none"></div>
    <button class="send-btn" id="sendBtn" onclick="sendMessage()">↑</button>
  </div>
</div>

<script>
  const contextPath = '<%= request.getContextPath() %>';
</script>
<script src="<%=request.getContextPath() %>/js/juego.js"></script>
</body>
</html>