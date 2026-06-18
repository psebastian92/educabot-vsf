package com.fatima.vientos;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/SalaServlet")
public class SalaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion == null || sesion.getAttribute("nombre") == null) {
            resp.setStatus(401);
            return;
        }

        // Marcar desconectadas las que no hicieron ping en 30 seg
        try (Connection con = ConectorBD.getConnection()) {
            String sqlDesconectar = "UPDATE sala_jugadoras SET conectada = 0 " +
                                    "WHERE TIMESTAMPDIFF(SECOND, ultimo_ping, NOW()) > 30";
            con.prepareStatement(sqlDesconectar).executeUpdate();

            // Traer estado de todas
            JSONArray jugadoras = new JSONArray();
            ResultSet rs = con.prepareStatement(
                "SELECT nombre, conectada, rol_elegido FROM sala_jugadoras"
            ).executeQuery();

            int conectadas = 0;
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("nombre", rs.getString("nombre"));
                j.put("conectada", rs.getInt("conectada") == 1);
                j.put("rol", rs.getString("rol_elegido"));
                jugadoras.put(j);
                if (rs.getInt("conectada") == 1) conectadas++;
            }

            JSONObject respuesta = new JSONObject();
            respuesta.put("jugadoras", jugadoras);
            respuesta.put("todasConectadas", conectadas == 5);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(respuesta.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(500);
        }
    }
}