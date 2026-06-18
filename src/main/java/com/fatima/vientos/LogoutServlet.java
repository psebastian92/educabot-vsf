package com.fatima.vientos;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/LogoutServlet")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion != null) {
            String nombre = (String) sesion.getAttribute("nombre");
            // Marcar como desconectada en BD
            try (Connection con = ConectorBD.getConnection()) {
                String sql = "UPDATE sala_jugadoras SET conectada = 0, rol_elegido = NULL, ultimo_ping = NULL WHERE nombre = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, nombre);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sesion.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/index.jsp");
    }
}