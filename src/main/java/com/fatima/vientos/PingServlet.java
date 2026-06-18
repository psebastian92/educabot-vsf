package com.fatima.vientos;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/PingServlet")
public class PingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("nombre") == null) {
            resp.setStatus(401);
            return;
        }

        String nombre = (String) sesion.getAttribute("nombre");

        try (Connection con = ConectorBD.getConnection()) {
            String sql = "UPDATE sala_jugadoras SET ultimo_ping = NOW() WHERE nombre = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        resp.setStatus(200);
    }
}