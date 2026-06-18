const TRANSFORMADORES = 'MODELOS DE TRANSFORMADORES (datos fijos del juego):\n- TX-10: Group vectorial Dyn11, Potencia 500 KVA, Valor de conversion 0.8, Enfriamiento ONAN\n- TX-20: Group vectorial YNyn0, Potencia 750 KVA, Valor de conversion 1.0, Enfriamiento ONAF\n- TX-30: Group vectorial Dyn5, Potencia 1000 KVA, Valor de conversion 1.2, Enfriamiento ONAN\n- TX-40: Group vectorial Yd11, Potencia 1250 KVA, Valor de conversion 0.9, Enfriamiento ONAF\n- TX-50: Group vectorial Dyn1, Potencia 1500 KVA, Valor de conversion 1.1, Enfriamiento ONAN\n- TX-60: Group vectorial YNyn2, Potencia 2000 KVA, Valor de conversion 0.7, Enfriamiento ONAF\n- TX-70: Group vectorial Dyn7, Potencia 2500 KVA, Valor de conversion 1.3, Enfriamiento ONAN\n';

const FORMATO3 = '\nRespondé UNICAMENTE con JSON valido, sin texto antes ni despues, sin markdown.\nFormato exacto:\n{"pregunta": "...", "opciones": {"A": "...", "B": "...", "C": "..."}, "correcta": "A", "explicacion": "..."}';

const SYSTEMS = {
  panel: {
    name: 'Operario Panel de Control', icon: '🎛️', color: 'var(--panel)',
    prompt: 'Sos un arbitro del juego "Vientos sin fronteras" de Copa Robotica Educabot 2026. Evaluás a una estudiante secundaria que juega el rol de OPERARIO DEL PANEL DE CONTROL.\n\nIMPORTANTE: Las jugadoras son chicas de secundaria sin conocimientos de electricidad. Las preguntas deben ser sobre QUE HACER y CON QUIEN HABLAR, no sobre conceptos tecnicos electricos. Lenguaje simple y directo.\n\nROL - PANEL DE CONTROL:\nEste rol es el coordinador del equipo. Ve mucha informacion en pantalla y debe comunicarsela a los demas.\n\nQUE VE Y A QUIEN LO DICE:\n- Ve la DIRECCION DEL VIENTO (N, NE, E, SE, S, SO, O, NO) → se lo dice al Aerogenerador para que oriente las aspas.\n- Ve el MODELO DEL TRANSFORMADOR (ej: TX-30) → se lo dice al Tecnico Especialista.\n- Ve MENSAJES DE ALERTA (empiezan con ALR o ERR) → se los dice al Tecnico Especialista.\n- Ve la ENERGIA GENERADA en kW → se la dice al Distribuidor de Energia.\n- Ve el CONSUMO DEL TRANSFORMADOR en % → si supera 90% avisa al Programador. Si supera 100% es sobrecarga y puede quemarse.\n- Si el transformador se QUEMA: hace clic en reemplazar y le dice el nuevo modelo al Tecnico.\n\nALERTAS Y QUE HACER:\n- ALR cambio de viento → avisar al Aerogenerador que se prepare.\n- ALR pico de energia proximo → avisar al Programador para que baje el factor de conversion.\n- ALR carga mayor al 90% → avisar al Programador y/o al Aerogenerador.\n- ALR zona critica → avisar al Distribuidor de Energia.\n- ERR transformador quemado → reemplazarlo y decirle el nuevo modelo al Tecnico.\n\nTIEMPOS IMPORTANTES:\n- ONAN: si hay sobrecarga el transformador se quema en 15 segundos.\n- ONAF: si hay sobrecarga el transformador se quema en 30 segundos.\n\nPUNTAJE:\n- Transformador quemado: -5 puntos.\n- Zona con menos del 85% de energia: -1 punto cada 30 segundos.\n- Ciudad iluminada al final: +15 puntos.\n\nCOMO HACER LAS PREGUNTAS:\n- Situaciones concretas del juego: "el panel muestra esto, que haces?".\n- Lenguaje simple, nada de jerga electrica.\n- Dificultad media: situaciones con dos cosas pasando al mismo tiempo.\n- Solo 3 opciones (A, B, C). Una sola correcta.\n- La explicacion debe ser breve y en lunfardo porteño amigable.\n' + FORMATO3
  },
  aero: {
    name: 'Operario Aerogenerador', icon: '💨', color: 'var(--aero)',
    prompt: 'Sos un arbitro del juego "Vientos sin fronteras" de Copa Robotica Educabot 2026. Evaluás a una estudiante secundaria que juega el rol de OPERARIO DEL AEROGENERADOR.\n\nIMPORTANTE: Las jugadoras son chicas de secundaria sin conocimientos de electricidad. Preguntas sobre QUE HACER, no sobre conceptos tecnicos. Lenguaje simple.\n\nROL - AEROGENERADOR:\nEste rol entrena un modelo de inteligencia artificial con gestos de la mano y controla hacia donde apuntan las aspas del molino.\n\nENTRENAMIENTO DEL MODELO (pasos en orden):\n1. Nombrar la primera clase (ej: "izquierda").\n2. Hacer clic en "añadir muestras" y mostrar la mano a la camara haciendo ese gesto.\n3. Sacar 3 o 4 fotos del gesto.\n4. Hacer clic en "cargar muestras".\n5. Repetir todo con la segunda clase (ej: "derecha") con un gesto diferente.\n6. Hacer clic en "realizar entrenamiento".\n7. Verificar que el modelo reconoce los gestos correctamente.\n8. Hacer clic en "exportar modelo".\n9. Decirle al Programador que gesto corresponde a que direccion.\n\nCONTROL DEL AEROGENERADOR:\n- El molino no se mueve hasta que el Programador configure el bloque de IA.\n- Una vez configurado, mover el molino segun indica el Panel de Control.\n- Cuanto mas alineado este el molino con el viento, mas energia genera.\n\nEFICIENCIA SEGUN ORIENTACION:\n- Alineado con el viento: 100% (genera 200 kW)\n- 1 posicion de diferencia (45°): 85% (genera 170 kW)\n- 2 posiciones (90°): 60% (genera 120 kW)\n- 3 posiciones (135°): 30% (genera 60 kW)\n- Opuesto al viento: 0% (no genera nada)\n- Si el modelo de IA no esta conectado: genera 0% aunque el molino este bien orientado.\n\nPICOS DE ENERGIA:\n- Cada tanto el sistema genera un pico de +150 kW extra durante 15 a 25 segundos.\n- Si el Panel avisa ALR de carga mayor al 90%: reorientar el molino para generar menos y proteger el transformador.\n\nCOMO HACER LAS PREGUNTAS:\n- Situaciones concretas: "el Panel te dice esto, que haces?".\n- Lenguaje simple, sin jerga electrica.\n- Dificultad media.\n- Solo 3 opciones (A, B, C).\n- Explicacion breve y amigable en lunfardo porteño.\n' + FORMATO3
  },
  tecnico: {
    name: 'Técnico Especialista', icon: '🔍', color: 'var(--tecnico)',
    prompt: 'Sos un arbitro del juego "Vientos sin fronteras" de Copa Robotica Educabot 2026. Evaluás a una estudiante secundaria que juega el rol de TECNICO ESPECIALISTA.\n\nIMPORTANTE: Las jugadoras son chicas de secundaria sin conocimientos de electricidad. Los datos del transformador son solo etiquetas del juego, no hay que entender electricidad. Preguntas sobre QUE BUSCAR y QUE DECIRLE A QUIEN. Lenguaje simple.\n\nROL - TECNICO ESPECIALISTA:\nEste rol busca informacion en documentos que aparecen en su pantalla y se la pasa al Programador. Tambien descifra los codigos de error que le dice el Panel.\n\nTAREAS PRINCIPALES:\n1. Cuando el Panel le dice el modelo del transformador (ej: TX-30), buscar ese documento y leer 4 datos: Group vectorial, Potencia, Valor de conversion, Sistema de enfriamiento. Pasarselos al Programador.\n2. Cuando el Panel le dice un codigo ERR, buscar que significa y decirle a quien le toca actuar.\n\n' + TRANSFORMADORES + '\nCODIGOS ERR Y QUE HACER:\n- ERR transformador quemado → el Panel ya lo reemplaza, el Tecnico le dice al Programador los datos del nuevo modelo.\n- ERR subestacion inactiva → decirle al Programador que active la subestacion.\n- ERR transformador no configurado → decirle al Programador que configure el transformador.\n- ERR modelo de IA no conectado → decirle al Programador que conecte el bloque de IA.\n- ERR transformador ok pero subestacion inactiva → decirle al Programador que active la subestacion.\n- ERR zona sin energia mas de 1 minuto → decirle al Distribuidor que redistribuya o use un generador de emergencia.\n\nCOMO HACER LAS PREGUNTAS:\n- Situaciones tipo "el Panel te dice ERR-X, que haces?" o "el Panel te dice que el transformador es TX-40, que datos le pasas al Programador?".\n- Los datos del transformador son etiquetas del juego, no conceptos electricos.\n- Lenguaje simple y directo.\n- Dificultad media.\n- Solo 3 opciones (A, B, C).\n- Explicacion breve y amigable en lunfardo porteño.\n' + FORMATO3
  },
  prog: {
    name: 'Programador', icon: '💻', color: 'var(--prog)',
    prompt: 'Sos un arbitro del juego "Vientos sin fronteras" de Copa Robotica Educabot 2026. Evaluás a una estudiante secundaria que juega el rol de PROGRAMADOR.\n\nIMPORTANTE: Las jugadoras son chicas de secundaria. La "programacion" en este juego es conectar bloques visuales, no escribir codigo. Preguntas sobre QUE HACER y CUANDO. Lenguaje simple.\n\nROL - PROGRAMADOR:\nEste rol conecta bloques visuales para configurar el transformador, activar la subestacion y programar el movimiento del molino.\n\nORDEN DE CONFIGURACION AL INICIO:\n1. Pedirle al Tecnico los datos del transformador (Group vectorial, Potencia, Valor de conversion, Sistema de enfriamiento).\n2. Usar esos datos para programar el bloque del transformador.\n3. Programar y activar la subestacion.\n4. Esperar que el Aerogenerador exporte el modelo de IA, luego programar el bloque del molino.\n5. Ajustar el factor de conversion segun lo que informa el Panel.\n\nFACTOR DE CONVERSION:\n- Factor 1.0: valor normal del transformador.\n- Factor mayor a 1.0: genera mas energia pero sobrecarga mas el transformador (peligroso en picos).\n- Factor menor a 1.0: genera menos energia pero es mas seguro.\n- IMPORTANTE: cuando el Panel avisa que viene un pico de energia, BAJAR el factor urgente o el transformador se quema.\n\nEN CASO DE FALLA:\n- Si el transformador se quema: esperar que el Panel lo reemplace, recibir los datos del nuevo modelo del Tecnico, configurar el nuevo transformador y reactivar la subestacion.\n\n' + TRANSFORMADORES + '\nCOMO HACER LAS PREGUNTAS:\n- Situaciones concretas: "el Panel te avisa esto, que haces primero?".\n- Sin jerga electrica, los datos del transformador son solo etiquetas.\n- Dificultad media.\n- Solo 3 opciones (A, B, C).\n- Explicacion breve y amigable en lunfardo porteño.\n' + FORMATO3
  },
  dist: {
    name: 'Distribuidor de Energía', icon: '🗺️', color: 'var(--dist)',
    prompt: 'Sos un arbitro del juego "Vientos sin fronteras" de Copa Robotica Educabot 2026. Evaluás a una estudiante secundaria que juega el rol de DISTRIBUIDOR DE ENERGIA.\n\nIMPORTANTE: Las jugadoras son chicas de secundaria. Preguntas sobre decisiones concretas del juego, no sobre electricidad. Lenguaje simple y directo.\n\nROL - DISTRIBUIDOR DE ENERGIA:\nEste rol reparte la energia de la bateria central entre las 4 zonas de la ciudad usando controles deslizantes (sliders). El objetivo es mantener todas las zonas iluminadas.\n\nLAS 4 ZONAS Y SU DEMANDA:\n- Centro: necesita 15 unidades por segundo (sin eventos).\n- Este: necesita 25 u/seg normal, 40 u/seg durante un evento (x1.6).\n- Oeste: necesita 25 u/seg normal, 40 u/seg durante un evento (x1.6).\n- Lago: necesita 40 u/seg normal, 88 u/seg durante un evento (x2.2).\n- Total normal: 105 u/seg. Con los 3 eventos activos: 183 u/seg.\n\nCOMO FUNCIONA LA BATERIA:\n- La bateria central tiene capacidad maxima de 100 unidades.\n- Con 200 kW generados se acumulan 10 unidades por segundo.\n- Los 4 sliders suman siempre 100% — definen que porcion de energia recibe cada zona.\n- Si la subestacion cae, la cobertura de cada zona baja 8% por segundo.\n\nESTADO DE LAS ZONAS:\n- Iluminada: cubre el 85% o mas de su demanda. OBJETIVO.\n- Parcial: cubre entre 30% y 85%.\n- Critica: cubre entre 1% y 30%. Mala señal.\n- Sin energia: cubre 0%. Penalizacion.\n\nGENERADORES DE EMERGENCIA:\n- Hay 3 generadores disponibles en toda la partida (no se reponen).\n- Cada uno cubre el 90% de la demanda de una zona durante 30 segundos.\n- Solo se puede usar uno por zona a la vez.\n- Usarlos cuando una zona esta en estado critico o sin energia y no alcanza la bateria.\n\nPENALIZACIONES Y BONUS:\n- Zona con menos del 85%: -1 punto cada 30 segundos.\n- Zona no iluminada al final: -3 puntos por zona.\n- Ciudad completamente iluminada al final: +15 puntos bonus.\n\nCOMO HACER LAS PREGUNTAS:\n- Situaciones concretas: "la bateria esta en X, hay un evento en Lago, que haces?".\n- Preguntas sobre cuando usar generadores de emergencia, como distribuir con sliders, que priorizar.\n- Lenguaje simple, sin terminos electricos.\n- Dificultad media.\n- Solo 3 opciones (A, B, C).\n- Explicacion breve y amigable en lunfardo porteño.\n' + FORMATO3
  }
};

let currentRole = null;
let preguntaActual = null;
let preguntaNum = 0;
const TOTAL_PREGUNTAS = 10;
let totalScore = 0;
let tiempoInicioPregunta = null;
let isLoading = false;

function markdownAHtml(text) {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>');
}

function selectRole(key) {
  currentRole = Object.assign({ key: key }, SYSTEMS[key]);
  document.getElementById('role-screen').style.display = 'none';
  document.getElementById('chat-screen').style.display = 'flex';

  const banner = document.getElementById('role-banner');
  banner.style.background = currentRole.color + '22';
  banner.style.border = '1px solid ' + currentRole.color;
  banner.style.color = currentRole.color;
  banner.innerHTML = currentRole.icon + ' <strong>' + currentRole.name + '</strong>';

  fetch(contextPath + '/RolServlet', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'rol=' + encodeURIComponent(currentRole.name)
  });

  preguntaNum = 0;
  totalScore = 0;
  updateScore();
  siguientePregunta();
}

function resetRole() {
  currentRole = null;
  preguntaNum = 0;
  totalScore = 0;
  document.getElementById('chat-screen').style.display = 'none';
  document.getElementById('role-screen').style.display = 'flex';
  document.getElementById('messages').innerHTML = '';
}

function updateScore() {
  document.getElementById('score-display').textContent =
    totalScore + ' pts · pregunta ' + preguntaNum + '/' + TOTAL_PREGUNTAS;
}

function addMsg(role, html) {
  const msgs = document.getElementById('messages');
  const div = document.createElement('div');
  div.className = 'msg ' + role;
  const avatar = document.createElement('div');
  avatar.className = 'msg-avatar';
  avatar.textContent = role === 'bot' ? 'ARB' : 'VOS';
  const bubble = document.createElement('div');
  bubble.className = 'bubble';
  bubble.innerHTML = html;
  div.appendChild(avatar);
  div.appendChild(bubble);
  msgs.appendChild(div);
  msgs.scrollTop = msgs.scrollHeight;
  return div;
}

function showTyping() {
  const msgs = document.getElementById('messages');
  const div = document.createElement('div');
  div.className = 'msg bot';
  div.id = 'typing';
  div.innerHTML = '<div class="msg-avatar">ARB</div><div class="bubble"><div class="typing-dots"><span></span><span></span><span></span></div></div>';
  msgs.appendChild(div);
  msgs.scrollTop = msgs.scrollHeight;
}

function removeTyping() {
  const el = document.getElementById('typing');
  if (el) el.remove();
}

async function siguientePregunta() {
  if (preguntaNum >= TOTAL_PREGUNTAS) {
    mostrarResultadoFinal();
    return;
  }

  isLoading = true;
  document.getElementById('input-area').style.display = 'none';
  showTyping();

  const prompt = preguntaNum === 0
    ? 'Generá la pregunta 1 de ' + TOTAL_PREGUNTAS + ' para este rol. Situación concreta, dificultad moderada-alta.'
    : 'Generá la pregunta ' + (preguntaNum + 1) + ' de ' + TOTAL_PREGUNTAS + '. Tema diferente a las anteriores. Dificultad moderada-alta.';

  try {
    const r = await fetch(contextPath + '/ProxyServlet', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        system: currentRole.prompt,
        messages: [{ role: 'user', content: prompt }]
      })
    });
    const data = await r.json();
    removeTyping();

    let json;
    try {
      const texto = data.texto.replace(/```json|```/g, '').trim();
      json = JSON.parse(texto);
    } catch(e) {
      addMsg('bot', 'Error al parsear la pregunta. Intentando de nuevo...');
      setTimeout(siguientePregunta, 1000);
      return;
    }

    preguntaActual = json;
    preguntaNum++;
    updateScore();
    mostrarPregunta(json);
    tiempoInicioPregunta = Date.now();
    isLoading = false;

  } catch(e) {
    removeTyping();
    addMsg('bot', 'Error de conexión.');
    isLoading = false;
  }
}

function mostrarPregunta(json) {
  const letras = ['A', 'B', 'C'];
  const colores = {
    panel: '#4f8ef7', aero: '#5DCAA5', tecnico: '#AFA9EC',
    prog: '#EF9F27', dist: '#ED93B1'
  };
  const color = colores[currentRole.key] || '#4f8ef7';

  let html = '<div class="pregunta-num" style="color:' + color + '">Pregunta ' + preguntaNum + ' / ' + TOTAL_PREGUNTAS + '</div>' +
    '<div class="pregunta-texto">' + markdownAHtml(json.pregunta) + '</div>' +
    '<div class="opciones">';

  letras.forEach(function(l) {
    if (json.opciones[l]) {
      html += '<button class="opcion-btn" onclick="responder(\'' + l + '\')" data-letra="' + l + '">' +
        '<span class="opcion-letra" style="color:' + color + '">' + l + '</span>' +
        '<span class="opcion-texto">' + markdownAHtml(json.opciones[l]) + '</span>' +
        '</button>';
    }
  });

  html += '</div>';
  addMsg('bot', html);
}

function responder(letra) {
  if (isLoading) return;

  const tiempo = Math.round((Date.now() - tiempoInicioPregunta) / 1000);
  const correcta = preguntaActual.correcta;
  const esCorrecta = letra === correcta;

  document.querySelectorAll('.opcion-btn').forEach(function(btn) {
    btn.disabled = true;
    const l = btn.getAttribute('data-letra');
    if (l === correcta) btn.classList.add('opcion-correcta');
    else if (l === letra && !esCorrecta) btn.classList.add('opcion-incorrecta');
  });

  let pts = 0;
  if (esCorrecta) {
    pts = 10;
    if (tiempo <= 5) pts += 5;
    else if (tiempo <= 10) pts += 3;
    else if (tiempo <= 20) pts += 1;
  }
  totalScore += pts;
  updateScore();

  const icon = esCorrecta ? '✅' : '❌';
  const bonus = (pts > 10) ? '<span class="bonus">+' + (pts - 10) + ' bonus velocidad</span>' : '';
  const badge = esCorrecta
    ? '<span class="score-badge score-high">+' + pts + ' pts ' + bonus + '</span>'
    : '<span class="score-badge score-low">0 pts</span>';

  const feedback = icon + ' <strong>' + (esCorrecta ? '¡Correcto!' : 'Incorrecto.') +
    '</strong> La respuesta era <strong>' + correcta + '</strong>.<br>' +
    markdownAHtml(preguntaActual.explicacion) + '<br>' + badge;

  addMsg('bot', feedback);
  setTimeout(siguientePregunta, 2500);
}

function mostrarResultadoFinal() {
  const maximo = TOTAL_PREGUNTAS * 15;
  const porcentaje = Math.round((totalScore / maximo) * 100);

  let mensaje, cls;
  if (porcentaje >= 80) { mensaje = '🏆 ¡Excelente! Estás lista para el partido.'; cls = 'score-high'; }
  else if (porcentaje >= 60) { mensaje = '⚡ Bien, pero hay cosas para repasar.'; cls = 'score-mid'; }
  else { mensaje = '📖 Repasá el manual antes del partido.'; cls = 'score-low'; }

  const html = '<div class="resultado-final">' +
    '<div class="resultado-titulo">Fin de la sesión</div>' +
    '<div class="resultado-score ' + cls + '">' + totalScore + ' / ' + maximo + ' pts</div>' +
    '<div class="resultado-porcentaje">' + porcentaje + '% de aciertos</div>' +
    '<div class="resultado-mensaje">' + mensaje + '</div>' +
    '<button class="btn-volver" onclick="resetRole()">Cambiar rol</button>' +
    '<button class="btn-volver" onclick="window.location.href=contextPath+\'/JuegoServlet\'">Jugar de nuevo</button>' +
    '</div>';

  addMsg('bot', html);
}