package com.fatima.vientos;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.Map;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

	private static final Map<String, String> CLAVES = Map.of("Isabella", "isabella2026", "Daira", "daira2026", "Alma",
			"alma2026", "Catalina", "catalina2026", "Kiara", "kiara2026");

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String nombre = req.getParameter("nombre");
		String clave = req.getParameter("clave");

		String claveCorrecta = CLAVES.get(nombre);

		if (claveCorrecta != null && claveCorrecta.equals(clave)) {
			// Crear sesión HTTP
			HttpSession sesion = req.getSession();
			sesion.setAttribute("nombre", nombre);
			sesion.setMaxInactiveInterval(1800); // 30 minutos

			// Marcar como conectada en BD
			try (Connection con = ConectorBD.getConnection()) {
				String sql = "UPDATE sala_jugadoras SET conectada = 1, ultimo_ping = NOW() WHERE nombre = ?";
				PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, nombre);
				ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			req.getRequestDispatcher("/WEB-INF/vistas/sala.jsp").forward(req, resp);

		} else {
			// Login fallido → volver al index con error
			req.setAttribute("error", "Usuario o clave incorrectos.");
			req.getRequestDispatcher("/index.jsp").forward(req, resp);
		}
	}
}