package com.fatima.vientos;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

/**
 * Router: mira qué rol tiene asignada la chica en sala_jugadoras
 * y la redirige al prototipo correspondiente.
 *
 * Reemplaza el viejo JuegoServlet del árbitro chatbot.
 */
@WebServlet("/JuegoServlet")
public class JuegoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, jakarta.servlet.ServletException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("nombre") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        String nombre = (String) sesion.getAttribute("nombre");
        String rol = null;

        // Buscar el rol elegido por esta chica
        try (Connection con = ConectorBD.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT rol_elegido FROM sala_jugadoras WHERE nombre = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) rol = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Si no eligió rol, volver a la sala
        if (rol == null || rol.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/WEB-INF/vistas/sala.jsp");
            return;
        }

        // Mapeo rol → prototipo
        String prototipo;
        switch (rol) {
            case "panel":   prototipo = "/panel_prototipo.html"; break;
            case "aero":    prototipo = "/aerogenerador_prototipo.html"; break;
            case "tecnico": prototipo = "/tecnico_prototipo.html"; break;
            case "prog":    prototipo = "/programador_prototipo.html"; break;
            case "dist":    prototipo = "/distribuidor_prototipo.html"; break;
            default:
                resp.sendRedirect(req.getContextPath() + "/index.jsp");
                return;
        }

        resp.sendRedirect(req.getContextPath() + prototipo);
    }
}