package com.fatima.vientos;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Devuelve TODO el estado del juego en un único JSON.
 * Las pantallas de los 5 roles consultan este endpoint y cada una usa
 * la parte que le corresponde mostrar.
 *
 * Estrategia: lo que cambia por acciones de jugadoras vive en BD;
 * lo que se mueve solo (viento, picos, alertas) se calcula al vuelo
 * con JuegoTiempo a partir del momento de inicio. Así los 5 navegadores
 * ven exactamente lo mismo sin sincronizar nada.
 */
@WebServlet("/EstadoServlet")
public class EstadoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try (Connection conn = ConectorBD.getConnection()) {

            // ----- 1) leer partida -----
            JuegoTiempo.EstadoPartida e = new JuegoTiempo.EstadoPartida();
            String estadoPartida = "esperando";
            int energiaTotal = 0;
            double bateria = 0;
            int puntaje = 0;
            int txQuemados = 0;
            int generadoresRestantes = 3;
            String gesto1Dir = null, gesto2Dir = null;
            String aeroDir = "N";
            boolean txActualmenteQuemado = false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT estado, inicio, modelo_tx_activo, tx_configurado, subestacion_on, " +
                    "ml_conectado, energia_total_kw, bateria, puntaje, transformadores_quemados, " +
                    "generadores_restantes, gesto1_direccion, gesto2_direccion, aerogenerador_direccion, " +
                    "tx_actualmente_quemado FROM partida WHERE id=1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    estadoPartida   = rs.getString("estado");
                    Timestamp t     = rs.getTimestamp("inicio");
                    e.inicio        = (t != null) ? t.toLocalDateTime() : null;
                    e.modeloTxActivo = rs.getString("modelo_tx_activo");
                    e.txConfigurado = rs.getInt("tx_configurado") == 1;
                    e.subestacionOn = rs.getInt("subestacion_on") == 1;
                    e.mlConectado   = rs.getInt("ml_conectado") == 1;
                    energiaTotal    = rs.getInt("energia_total_kw");
                    bateria         = rs.getDouble("bateria");
                    puntaje         = rs.getInt("puntaje");
                    txQuemados      = rs.getInt("transformadores_quemados");
                    generadoresRestantes = rs.getInt("generadores_restantes");
                    gesto1Dir       = rs.getString("gesto1_direccion");
                    gesto2Dir       = rs.getString("gesto2_direccion");
                    String d        = rs.getString("aerogenerador_direccion");
                    if (d != null && !d.isEmpty()) aeroDir = d;
                    txActualmenteQuemado = rs.getInt("tx_actualmente_quemado") == 1;
                }
            }

            // ----- 2) leer datos del transformador activo -----
            int potenciaMva = 0;
            String grupoVect = "", enfriamiento = "", conversion = "", fabricante = "";
            if (e.modeloTxActivo != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT potencia_mva, conversion_alta, conversion_baja, " +
                        "grupo_vectorial, enfriamiento, fabricante FROM transformador WHERE modelo=?")) {
                    ps.setString(1, e.modeloTxActivo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            potenciaMva = rs.getInt("potencia_mva");
                            int ca = rs.getInt("conversion_alta");
                            int cb = rs.getInt("conversion_baja");
                            conversion   = ca + "/" + cb + " kV";
                            grupoVect    = rs.getString("grupo_vectorial");
                            enfriamiento = rs.getString("enfriamiento");
                            fabricante   = rs.getString("fabricante");
                        }
                    }
                }
            }

            // ----- 3) calcular cosas derivadas del tiempo -----
            String viento = JuegoTiempo.direccionViento(e.inicio);
            // La dirección del aerogenerador viene de BD (aerogenerador_direccion).
            // Por defecto arranca en N, y la chica la modifica con gestos.
            String dirAero = aeroDir;
            int eficiencia = JuegoTiempo.eficienciaAerogenerador(viento, dirAero);
            boolean picoAhora = JuegoTiempo.picoActivo(e.inicio, e.inicio); // simplificado
            int kw = JuegoTiempo.kwActuales(e.txConfigurado, e.subestacionOn, e.mlConectado, eficiencia, picoAhora);
            e.cargaActual = JuegoTiempo.porcentajeCarga(kw, potenciaMva);
            e.txQuemado = txActualmenteQuemado;
            e.subestacionDesde = e.inicio;

            List<JuegoTiempo.Alerta> alertas = JuegoTiempo.alertasActivas(e);

            // ----- 4) leer zonas -----
            // Antes de leer, apagar generadores que ya pasaron sus 30 seg (manual sec 4.7.5)
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE zona SET generador_activo=0, generador_inicio=NULL " +
                    "WHERE generador_activo=1 AND generador_inicio IS NOT NULL " +
                    "AND TIMESTAMPDIFF(SECOND, generador_inicio, NOW()) >= 30")) {
                ps.executeUpdate();
            }

            // ----- TICK DEL JUEGO -----
            // Avanzar el estado del juego según segundos transcurridos desde último tick.
            // Solo si la partida está jugándose.
            if ("jugando".equals(estadoPartida) && e.inicio != null) {
                double[] resultado = tickJuego(conn, bateria, kw, e.cargaActual, enfriamiento, puntaje, txActualmenteQuemado);
                bateria = resultado[0];
                puntaje = (int) resultado[1];
                // Si tickJuego quemó el transformador, reflejarlo en el estado actual
                if (resultado[2] == 1) {
                    e.txQuemado = true;
                    e.txConfigurado = false;
                    e.subestacionOn = false;
                    // El kw se recalcula como 0 porque ya no hay subestación
                    kw = 0;
                    e.cargaActual = 0;
                }
            }

            List<String> zonasJson = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nombre, demanda_base, multiplicador, tiene_evento, slider_pct, " +
                    "cobertura_pct, generador_activo FROM zona");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    zonasJson.add(
                        "{\"nombre\":\""    + rs.getString("nombre") + "\"," +
                        "\"demandaBase\":"  + rs.getInt("demanda_base") + "," +
                        "\"multiplicador\":" + rs.getDouble("multiplicador") + "," +
                        "\"tieneEvento\":"  + (rs.getInt("tiene_evento") == 1) + "," +
                        "\"slider\":"       + rs.getInt("slider_pct") + "," +
                        "\"cobertura\":"    + rs.getDouble("cobertura_pct") + "," +
                        "\"genActivo\":"    + (rs.getInt("generador_activo") == 1) + "}");
                }
            }

            // ----- 5) armar JSON final -----
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"estado\":\"").append(estadoPartida).append("\",");
            json.append("\"tiempoRestante\":\"").append(JuegoTiempo.tiempoRestante(e.inicio)).append("\",");
            json.append("\"puntaje\":").append(puntaje).append(",");

            // Bloque del Panel de Control
            json.append("\"panel\":{");
            json.append("\"viento\":\"").append(viento).append("\",");
            json.append("\"aerogeneradorDireccion\":\"").append(dirAero).append("\",");
            json.append("\"eficiencia\":").append(eficiencia).append(",");
            json.append("\"energiaActual\":").append(kw).append(",");
            json.append("\"energiaTotal\":").append(energiaTotal).append(",");
            json.append("\"cargaActual\":").append(e.cargaActual).append(",");
            json.append("\"subestacionOn\":").append(e.subestacionOn).append(",");
            json.append("\"transformador\":{");
            json.append("\"modelo\":\"").append(safe(e.modeloTxActivo)).append("\",");
            json.append("\"quemado\":").append(e.txQuemado);
            json.append("}");
            json.append("},");

            // Bloque del Técnico (datos completos del transformador)
            json.append("\"tecnico\":{");
            json.append("\"transformadorActivo\":{");
            json.append("\"modelo\":\"").append(safe(e.modeloTxActivo)).append("\",");
            json.append("\"potenciaMva\":").append(potenciaMva).append(",");
            json.append("\"conversion\":\"").append(conversion).append("\",");
            json.append("\"grupoVectorial\":\"").append(grupoVect).append("\",");
            json.append("\"enfriamiento\":\"").append(enfriamiento).append("\",");
            json.append("\"fabricante\":\"").append(fabricante).append("\"");
            json.append("}");
            json.append("},");

            // Bloque del Programador
            json.append("\"programador\":{");
            json.append("\"txConfigurado\":").append(e.txConfigurado).append(",");
            json.append("\"subestacionOn\":").append(e.subestacionOn).append(",");
            json.append("\"mlConectado\":").append(e.mlConectado).append(",");
            json.append("\"gesto1Direccion\":\"").append(safe(gesto1Dir)).append("\",");
            json.append("\"gesto2Direccion\":\"").append(safe(gesto2Dir)).append("\"");
            json.append("},");

            // Bloque del Distribuidor
            json.append("\"distribuidor\":{");
            json.append("\"bateria\":").append(bateria).append(",");
            json.append("\"generadoresRestantes\":").append(generadoresRestantes).append(",");
            json.append("\"zonas\":[");
            for (int i = 0; i < zonasJson.size(); i++) {
                if (i > 0) json.append(",");
                json.append(zonasJson.get(i));
            }
            json.append("]");
            json.append("},");

            // Alertas (visibles al Panel, decodificadas por el Técnico)
            json.append("\"alertas\":[");
            for (int i = 0; i < alertas.size(); i++) {
                JuegoTiempo.Alerta a = alertas.get(i);
                if (i > 0) json.append(",");
                json.append("{\"codigo\":\"").append(a.codigo)
                    .append("\",\"tipo\":\"").append(a.tipo)
                    .append("\",\"motivo\":\"").append(a.motivo).append("\"}");
            }
            json.append("]");

            json.append("}");

            out.print(json.toString());

        } catch (SQLException ex) {
            resp.setStatus(500);
            out.print("{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Avanza el estado del juego según el tiempo transcurrido.
     * Devuelve [bateriaNueva, puntajeNuevo, txAcabaDeQuemarse(0|1)]
     */
    private double[] tickJuego(Connection conn, double bateriaActual, int kwActuales,
                                int cargaActual, String enfriamiento,
                                int puntajeActual, boolean txYaQuemado) throws SQLException {
        // 1) Milisegundos transcurridos desde el último tick
        long ahora = System.currentTimeMillis();
        long ultimoTickMs = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ultimo_tick_ms FROM partida WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) ultimoTickMs = rs.getLong("ultimo_tick_ms");
        }

        // Primera vez: inicializar y salir
        if (ultimoTickMs == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE partida SET ultimo_tick_ms=? WHERE id=1")) {
                ps.setLong(1, ahora);
                ps.executeUpdate();
            }
            return new double[]{bateriaActual, puntajeActual, 0};
        }

        long deltaMs = ahora - ultimoTickMs;
        if (deltaMs < 500) return new double[]{bateriaActual, puntajeActual, 0};
        if (deltaMs > 5000) deltaMs = 5000;
        double segundosTick = deltaMs / 1000.0;

        // 2) Procesar EVENTOS en zonas (manual sec 4.7.4)
        // a) Terminar eventos vencidos (evento_fin <= NOW())
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE zona SET tiene_evento=0, evento_fin=NULL " +
                "WHERE tiene_evento=1 AND evento_fin IS NOT NULL AND evento_fin <= NOW()")) {
            ps.executeUpdate();
        }

        // b) Activar un nuevo evento si tocó (proximo_evento <= NOW())
        boolean activarEvento = false;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT proximo_evento FROM partida WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Timestamp pe = rs.getTimestamp("proximo_evento");
                if (pe == null || pe.getTime() <= System.currentTimeMillis()) activarEvento = true;
            }
        }
        if (activarEvento) {
            // Elegir una zona elegible: no Centro, sin evento activo, multiplicador > 1
            List<String> elegibles = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nombre FROM zona WHERE nombre <> 'Centro' AND tiene_evento = 0 AND multiplicador > 1");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) elegibles.add(rs.getString(1));
            }
            if (!elegibles.isEmpty()) {
                Collections.shuffle(elegibles);
                String elegida = elegibles.get(0);
                int duracionSeg = 20 + new Random().nextInt(26); // 20-45 seg
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE zona SET tiene_evento=1, evento_fin = DATE_ADD(NOW(), INTERVAL ? SECOND) WHERE nombre=?")) {
                    ps.setInt(1, duracionSeg);
                    ps.setString(2, elegida);
                    ps.executeUpdate();
                }
            }
            // Programar próximo evento entre 30-90 seg
            int gapSeg = 30 + new Random().nextInt(61);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE partida SET proximo_evento = DATE_ADD(NOW(), INTERVAL ? SECOND) WHERE id=1")) {
                ps.setInt(1, gapSeg);
                ps.executeUpdate();
            }
        }

        // 3) Llenar batería con la energía generada
        double bateriaNueva = bateriaActual + (kwActuales * 0.08 * segundosTick);
        if (bateriaNueva > 100) bateriaNueva = 100;

        // 4) Leer zonas (con tieneEvento actualizado)
        List<String> nombres = new ArrayList<>();
        List<double[]> zonas = new ArrayList<>(); // [demandaBase, multiplicador, slider, genActivo, tieneEvento, segSinEnergia]
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nombre, demanda_base, multiplicador, slider_pct, generador_activo, tiene_evento, segundos_sin_energia FROM zona");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                nombres.add(rs.getString("nombre"));
                zonas.add(new double[]{
                    rs.getInt("demanda_base"),
                    rs.getDouble("multiplicador"),
                    rs.getInt("slider_pct"),
                    rs.getInt("generador_activo") == 1 ? 1 : 0,
                    rs.getInt("tiene_evento") == 1 ? 1 : 0,
                    rs.getDouble("segundos_sin_energia")
                });
            }
        }

        double[] coberturas = new double[zonas.size()];
        double[] nuevosSegSinEnergia = new double[zonas.size()];

        // 5) Cada zona consume de la batería
        int puntajeNuevo = puntajeActual;
        for (int i = 0; i < zonas.size(); i++) {
            double[] z = zonas.get(i);
            double demandaBase   = z[0];
            double multZona      = z[1];
            double porcSlider    = z[2] / 100.0;
            boolean genActivo    = z[3] == 1;
            boolean enEvento     = z[4] == 1;
            double segSinEnergia = z[5];

            // Demanda real: con o sin evento (1.0 si no hay evento)
            double multReal = enEvento ? multZona : 1.0;
            double demandaPorSeg = demandaBase * multReal;
            double demandaTotal  = demandaPorSeg * segundosTick;

            // Pedir batería
            double quierePedir = demandaTotal * porcSlider;
            double tomado = Math.min(quierePedir, bateriaNueva);
            bateriaNueva -= tomado;

            double aporteGen = genActivo ? demandaTotal * 0.9 : 0;
            double recibido = tomado + aporteGen;

            double cobertura = (demandaTotal > 0) ? (recibido / demandaTotal) * 100 : 0;
            if (cobertura > 100) cobertura = 100;
            if (cobertura < 0)   cobertura = 0;
            coberturas[i] = cobertura;

            // PUNTAJE: +1 por segundo si está iluminada (≥85%)
            if (cobertura >= 85) {
                puntajeNuevo += (int) segundosTick;
                nuevosSegSinEnergia[i] = 0;
            } else if (cobertura == 0) {
                // PUNTAJE: -3 cada 30 seg sin energía
                segSinEnergia += segundosTick;
                while (segSinEnergia >= 30) {
                    puntajeNuevo -= 3;
                    segSinEnergia -= 30;
                }
                nuevosSegSinEnergia[i] = segSinEnergia;
            } else {
                nuevosSegSinEnergia[i] = 0;
            }
        }

        if (bateriaNueva < 0) bateriaNueva = 0;
        if (bateriaNueva > 100) bateriaNueva = 100;

        // 6) QUEMADO del transformador por sobrecarga (manual sec 4.3.3)
        int txAcabaDeQuemarse = 0;
        if (!txYaQuemado) {
            int limiteSeg = "ONAF".equalsIgnoreCase(enfriamiento) ? 30 : 15;
            if (cargaActual >= 100) {
                // Iniciar/seguir contando sobrecarga
                Timestamp sobreDesde = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT tx_sobrecarga_desde FROM partida WHERE id=1");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) sobreDesde = rs.getTimestamp("tx_sobrecarga_desde");
                }
                if (sobreDesde == null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE partida SET tx_sobrecarga_desde=NOW() WHERE id=1")) {
                        ps.executeUpdate();
                    }
                } else {
                    long segSobrecarga = (System.currentTimeMillis() - sobreDesde.getTime()) / 1000;
                    if (segSobrecarga >= limiteSeg) {
                        // ¡SE QUEMA!
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE partida SET tx_actualmente_quemado=1, tx_configurado=0, " +
                                "subestacion_on=0, tx_sobrecarga_desde=NULL, " +
                                "transformadores_quemados = transformadores_quemados + 1 WHERE id=1")) {
                            ps.executeUpdate();
                        }
                        txAcabaDeQuemarse = 1;
                        puntajeNuevo -= 5; // PUNTAJE: -5 por quemar
                    }
                }
            } else {
                // Carga bajó: resetear contador
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE partida SET tx_sobrecarga_desde=NULL WHERE id=1 AND tx_sobrecarga_desde IS NOT NULL")) {
                    ps.executeUpdate();
                }
            }
        }

        // 7) Persistir cobertura y segundos_sin_energia de cada zona
        for (int i = 0; i < zonas.size(); i++) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE zona SET cobertura_pct=?, segundos_sin_energia=? WHERE nombre=?")) {
                ps.setDouble(1, coberturas[i]);
                ps.setDouble(2, nuevosSegSinEnergia[i]);
                ps.setString(3, nombres.get(i));
                ps.executeUpdate();
            }
        }

        // 8) Persistir batería, puntaje, ultimo_tick
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE partida SET bateria=?, puntaje=?, ultimo_tick_ms=? WHERE id=1")) {
            ps.setDouble(1, bateriaNueva);
            ps.setInt(2, puntajeNuevo);
            ps.setLong(3, ahora);
            ps.executeUpdate();
        }

        return new double[]{bateriaNueva, puntajeNuevo, txAcabaDeQuemarse};
    }
}