package com.fatima.vientos;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Recibe acciones de jugadoras (POST) y modifica la BD.
 * Acciones soportadas:
 *   reemplazar_tx     → Panel cambia el transformador activo
 *   configurar_tx     → Programador (futuro)
 *   activar_sub       → Programador (futuro)
 *   conectar_ml       → Programador (futuro)
 *   mover_slider      → Distribuidor (futuro)
 *   activar_gen       → Distribuidor (futuro)
 */
@WebServlet("/AccionServlet")
public class AccionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String accion = req.getParameter("accion");

        if (accion == null) {
            resp.setStatus(400);
            out.print("{\"error\":\"falta parámetro accion\"}");
            return;
        }

        try (Connection conn = ConectorBD.getConnection()) {
            switch (accion) {
                case "reemplazar_tx":
                    reemplazarTransformador(conn, out);
                    break;
                case "configurar_tx":
                    configurarTransformador(conn, req, out);
                    break;
                case "activar_sub":
                    activarSubestacion(conn, out);
                    break;
                case "conectar_ml":
                    conectarML(conn, req, out);
                    break;
                case "mover_slider":
                    moverSlider(conn, req, out);
                    break;
                case "activar_gen":
                    activarGenerador(conn, req, out);
                    break;
                default:
                    resp.setStatus(400);
                    out.print("{\"error\":\"acción no reconocida: " + accion + "\"}");
            }
        } catch (SQLException ex) {
            resp.setStatus(500);
            out.print("{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
        }
    }

    /**
     * Elige un transformador distinto al actual y lo asigna como activo.
     * Resetea tx_configurado y subestacion_on porque el nuevo modelo
     * necesita ser configurado de cero (manual sec 4.3.1).
     */
    private void reemplazarTransformador(Connection conn, PrintWriter out) throws SQLException {
        // Modelo actual
        String actual = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT modelo_tx_activo FROM partida WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) actual = rs.getString(1);
        }

        // Lista de todos los modelos disponibles
        List<String> disponibles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT modelo FROM transformador");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) disponibles.add(rs.getString(1));
        }
        if (actual != null) disponibles.remove(actual);
        if (disponibles.isEmpty()) {
            out.print("{\"error\":\"no hay otros modelos disponibles\"}");
            return;
        }

        Collections.shuffle(disponibles);
        String nuevo = disponibles.get(0);

        // Actualizar partida
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE partida SET modelo_tx_activo=?, tx_configurado=0, subestacion_on=0, " +
                "transformadores_quemados = transformadores_quemados + 1 WHERE id=1")) {
            ps.setString(1, nuevo);
            ps.executeUpdate();
        }

        out.print("{\"ok\":true,\"nuevoModelo\":\"" + nuevo + "\"}");
    }

    private void setBool(Connection conn, String campo, int valor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE partida SET " + campo + "=? WHERE id=1")) {
            ps.setInt(1, valor);
            ps.executeUpdate();
        }
    }

    /**
     * El Programador intenta configurar el transformador con los datos que
     * el Técnico le pasó. Validamos que esos datos coincidan con el modelo
     * activo. Si no coinciden, devolvemos error sin tocar tx_configurado
     * (en el juego real eso dispara el ERR-08).
     *
     * Parámetros esperados: grupoVectorial, potenciaMva, conversionAlta, conversionBaja.
     */
    private void configurarTransformador(Connection conn, HttpServletRequest req, PrintWriter out)
            throws SQLException {
        String modeloActivo = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT modelo_tx_activo FROM partida WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) modeloActivo = rs.getString(1);
        }
        if (modeloActivo == null || modeloActivo.isEmpty()) {
            out.print("{\"error\":\"no hay transformador activo en la partida\"}");
            return;
        }

        String grupoIn = req.getParameter("grupoVectorial");
        String potIn   = req.getParameter("potenciaMva");
        String altaIn  = req.getParameter("conversionAlta");
        String bajaIn  = req.getParameter("conversionBaja");

        if (grupoIn == null || potIn == null || altaIn == null || bajaIn == null) {
            out.print("{\"error\":\"faltan parámetros: grupoVectorial, potenciaMva, conversionAlta, conversionBaja\"}");
            return;
        }

        // Leer datos reales del modelo activo
        int potReal = 0, altaReal = 0, bajaReal = 0;
        String grupoReal = "";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT potencia_mva, conversion_alta, conversion_baja, grupo_vectorial " +
                "FROM transformador WHERE modelo=?")) {
            ps.setString(1, modeloActivo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    potReal   = rs.getInt(1);
                    altaReal  = rs.getInt(2);
                    bajaReal  = rs.getInt(3);
                    grupoReal = rs.getString(4);
                }
            }
        }

        // Comparar
        int potOk, altaOk, bajaOk;
        try {
            potOk  = Integer.parseInt(potIn.trim());
            altaOk = Integer.parseInt(altaIn.trim());
            bajaOk = Integer.parseInt(bajaIn.trim());
        } catch (NumberFormatException ex) {
            out.print("{\"error\":\"valores numéricos inválidos\"}");
            return;
        }

        boolean coincide =
            grupoReal.equalsIgnoreCase(grupoIn.trim()) &&
            potReal == potOk &&
            altaReal == altaOk &&
            bajaReal == bajaOk;

        if (!coincide) {
            // Devolver detalle de qué no coincidió, sin revelar los valores correctos
            out.print("{\"error\":\"datos no coinciden con el transformador activo\"," +
                "\"esperado\":{\"modelo\":\"" + modeloActivo + "\"}}");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE partida SET tx_configurado=1 WHERE id=1")) {
            ps.executeUpdate();
        }
        out.print("{\"ok\":true}");
    }

    /**
     * Activar subestación solo si el transformador YA está configurado.
     * Manual sec 4.5: la subestación se activa cuando el Programador la programa
     * y la configuración del transformador es correcta.
     */
    private void activarSubestacion(Connection conn, PrintWriter out) throws SQLException {
        boolean txConfig = false;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT tx_configurado FROM partida WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) txConfig = rs.getInt(1) == 1;
        }
        if (!txConfig) {
            out.print("{\"error\":\"primero hay que configurar el transformador\"}");
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE partida SET subestacion_on=1 WHERE id=1")) {
            ps.executeUpdate();
        }
        out.print("{\"ok\":true}");
    }

    /**
     * Conecta el bloque ML del aerogenerador y guarda la asignación gesto→dirección
     * que la chica del Programador definió.
     *
     * Parámetros esperados: gesto1Direccion ('izquierda' o 'derecha'), gesto2Direccion.
     */
    private void conectarML(Connection conn, HttpServletRequest req, PrintWriter out)
            throws SQLException {
        String g1 = req.getParameter("gesto1Direccion");
        String g2 = req.getParameter("gesto2Direccion");

        if (g1 == null || g2 == null) {
            out.print("{\"error\":\"faltan parámetros gesto1Direccion y gesto2Direccion\"}");
            return;
        }
        if (!("izquierda".equals(g1) || "derecha".equals(g1)) ||
            !("izquierda".equals(g2) || "derecha".equals(g2))) {
            out.print("{\"error\":\"valores válidos: 'izquierda' o 'derecha'\"}");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE partida SET ml_conectado=1, gesto1_direccion=?, gesto2_direccion=? WHERE id=1")) {
            ps.setString(1, g1);
            ps.setString(2, g2);
            ps.executeUpdate();
        }
        out.print("{\"ok\":true}");
    }

    /**
     * Mueve el slider de una zona específica. Recibe params: zona (nombre) y valor (0-100).
     * No valida que sumen 100 entre las 4 — eso lo controla el frontend porque
     * en el juego las chicas las mueven independientemente.
     */
    private void moverSlider(Connection conn, HttpServletRequest req, PrintWriter out)
            throws SQLException {
        String zona = req.getParameter("zona");
        String valorStr = req.getParameter("valor");
        if (zona == null || valorStr == null) {
            out.print("{\"error\":\"faltan parámetros zona o valor\"}");
            return;
        }
        int valor;
        try {
            valor = Integer.parseInt(valorStr);
        } catch (NumberFormatException ex) {
            out.print("{\"error\":\"valor no es numérico\"}");
            return;
        }
        if (valor < 0) valor = 0;
        if (valor > 100) valor = 100;

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE zona SET slider_pct=? WHERE nombre=?")) {
            ps.setInt(1, valor);
            ps.setString(2, zona);
            ps.executeUpdate();
        }
        out.print("{\"ok\":true}");
    }

    /**
     * Activa el generador de emergencia en una zona. Dura 30 seg (controlado por
     * generador_inicio que se compara con NOW() en EstadoServlet).
     * Solo se puede activar si quedan generadores restantes (manual sec 4.7.5).
     */
    private void activarGenerador(Connection conn, HttpServletRequest req, PrintWriter out)
            throws SQLException {
        String zona = req.getParameter("zona");
        if (zona == null) {
            out.print("{\"error\":\"falta parámetro zona\"}");
            return;
        }

        // Verificar que quedan generadores disponibles
        int restantes = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT generadores_restantes FROM partida WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) restantes = rs.getInt(1);
        }
        if (restantes <= 0) {
            out.print("{\"error\":\"no quedan generadores disponibles\"}");
            return;
        }

        // Verificar que esta zona no tenga ya uno activo
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT generador_activo FROM zona WHERE nombre=?")) {
            ps.setString(1, zona);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 1) {
                    out.print("{\"error\":\"esa zona ya tiene un generador activo\"}");
                    return;
                }
            }
        }

        // Activar el generador en la zona
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE zona SET generador_activo=1, generador_inicio=NOW() WHERE nombre=?")) {
            ps.setString(1, zona);
            ps.executeUpdate();
        }

        // Descontar uno del stock de la partida
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE partida SET generadores_restantes = generadores_restantes - 1 WHERE id=1")) {
            ps.executeUpdate();
        }

        out.print("{\"ok\":true,\"restantes\":" + (restantes - 1) + "}");
    }
}