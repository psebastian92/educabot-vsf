package com.fatima.vientos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConectorBD {

    private static final String URL    = "jdbc:mysql://localhost:3306/educabot";
    private static final String USUARIO = "root";
    private static final String CLAVE   = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado", e);
        }
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}