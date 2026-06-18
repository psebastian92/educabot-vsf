let miRolActual = null;

function actualizarSala() {
	fetch(contextPath + '/SalaServlet')
		.then(r => r.json())
		.then(data => {
			const grid = document.getElementById('grid');
			grid.innerHTML = '';

			let conectadas = 0;
			// Mapa rol → nombre de quien lo eligió
			const rolesElegidos = {};

			data.jugadoras.forEach(j => {
				if (j.conectada) conectadas++;
				if (j.rol) rolesElegidos[j.rol] = j.nombre;
				if (j.nombre === miNombre) miRolActual = j.rol || null;

				const esVos = j.nombre === miNombre;
				const card = document.createElement('div');
				card.className = 'jugadora-card' + (j.conectada ? ' conectada' : '');
				card.innerHTML = `
					<div class="avatar">${j.nombre.charAt(0)}</div>
					<div class="jugadora-nombre">${j.nombre} ${esVos ? '<span class="vos-tag">(vos)</span>' : ''}</div>
					<div class="estado ${j.conectada ? 'online' : 'offline'}">${j.conectada ? 'conectada' : 'esperando...'}</div>
					${j.rol ? '<div class="rol-tag">' + nombreRol(j.rol) + '</div>' : ''}
				`;
				grid.appendChild(card);
			});

			document.getElementById('contador').textContent = conectadas;

			// Habilitar "Jugar mi rol" solo si ya elegiste rol
			const btnJugar = document.getElementById('btnJugar');
			if (miRolActual) {
				btnJugar.classList.add('habilitado');
				btnJugar.textContent = '⚡ Jugar como ' + nombreRol(miRolActual);
			} else {
				btnJugar.classList.remove('habilitado');
				btnJugar.textContent = '⚡ Elegí tu rol primero';
			}

			// Actualizar visualmente cada card de rol
			['panel', 'aero', 'tecnico', 'prog', 'dist'].forEach(rol => {
				const card = document.querySelector('.rol-card[data-rol="' + rol + '"]');
				const tomadoPor = rolesElegidos[rol];
				const indicador = document.getElementById('rol-tomado-' + rol);
				const btn = card.querySelector('.btn-elegir-rol');

				card.classList.remove('mi-rol', 'tomado');
				indicador.classList.remove('tomado', 'tuyo');
				btn.classList.remove('tuyo');

				if (tomadoPor === miNombre) {
					card.classList.add('mi-rol');
					indicador.classList.add('tuyo');
					indicador.textContent = 'Tu rol elegido';
					btn.classList.add('tuyo');
					btn.textContent = '✓ Mi rol';
					btn.disabled = false;
				} else if (tomadoPor) {
					card.classList.add('tomado');
					indicador.classList.add('tomado');
					indicador.textContent = 'Tomado por ' + tomadoPor;
					btn.textContent = 'Tomado';
					btn.disabled = true;
				} else {
					indicador.textContent = '';
					btn.textContent = 'Elegir este rol';
					btn.disabled = false;
				}
			});

			// Mensaje de cabecera
			const sub = document.getElementById('subtitulo');
			if (conectadas === 5) {
				sub.textContent = '¡Todas conectadas! Elegí tu rol y jugá.';
			} else {
				sub.textContent = 'Esperando que se conecten todas las jugadoras... (' + conectadas + '/5)';
			}
		});
}

function nombreRol(rol) {
	const nombres = {
		panel: 'Panel de Control',
		aero: 'Aerogenerador',
		tecnico: 'Técnico Especialista',
		prog: 'Programador',
		dist: 'Distribuidor de Energía'
	};
	return nombres[rol] || rol;
}

function elegirRol(rol) {
	const formData = new URLSearchParams();
	formData.append('rol', rol);

	fetch(contextPath + '/RolServlet', {
		method: 'POST',
		body: formData,
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
	}).then(r => {
		if (r.ok) actualizarSala();
		else alert('No se pudo elegir el rol');
	});
}

setInterval(() => fetch(contextPath + '/PingServlet', { method: 'POST' }), 10000);
actualizarSala();
setInterval(actualizarSala, 4000);
