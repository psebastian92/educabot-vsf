package com.fatima.vientos;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/RolServlet")
public class RolServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("nombre") == null) {
            resp.setStatus(401);
            return;
        }

        String nombre = (String) sesion.getAttribute("nombre");
        String rol = req.getParameter("rol");

        try (Connection con = ConectorBD.getConnection()) {
            String sql = "UPDATE sala_jugadoras SET rol_elegido = ? WHERE nombre = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, rol);
            ps.setString(2, nombre);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        resp.setStatus(200);
    }
}