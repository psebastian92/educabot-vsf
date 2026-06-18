package com.fatima.vientos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Calcula al vuelo todo lo que "se mueve solo" en la partida en base
 * al tiempo transcurrido desde su inicio. Lo hacemos así, en vez de
 * persistir en BD, para que los 5 roles vean exactamente lo mismo sin
 * sincronizar nada — el tiempo es el único reloj compartido.
 *
 * Usa semillas pseudoaleatorias derivadas del inicio de la partida,
 * así llamadas distintas con el mismo tiempo dan el mismo resultado.
 */
public class JuegoTiempo {

    private static final String[] DIRECCIONES = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
    public static final int DURACION_PARTIDA_SEG = 15 * 60; // 15 minutos

    /** Segundos transcurridos desde el inicio (clamp a duración total). */
    public static long segundosTranscurridos(LocalDateTime inicio) {
        if (inicio == null) return 0;
        long s = Duration.between(inicio, LocalDateTime.now()).getSeconds();
        if (s < 0) return 0;
        if (s > DURACION_PARTIDA_SEG) return DURACION_PARTIDA_SEG;
        return s;
    }

    /** Segundos restantes en formato "MM:SS". El cronómetro real arranca en 14:55. */
    public static String tiempoRestante(LocalDateTime inicio) {
        if (inicio == null) return "15:00";
        long transcurridos = segundosTranscurridos(inicio);
        long restantes = DURACION_PARTIDA_SEG - 5 - transcurridos; // arranca en 14:55
        if (restantes < 0) restantes = 0;
        long mm = restantes / 60;
        long ss = restantes % 60;
        return String.format("%02d:%02d", mm, ss);
    }

    /**
     * Dirección del viento en este momento. Cambia cada 20-50 seg,
     * nunca repite la dirección anterior. Determinístico según semilla del inicio.
     */
    public static String direccionViento(LocalDateTime inicio) {
        if (inicio == null) return "N";
        long t = segundosTranscurridos(inicio);
        long semilla = inicio.toLocalTime().toSecondOfDay();

        Random r = new Random(semilla);
        String actual = "N";
        long acumulado = 0;

        while (acumulado <= t) {
            long duracion = 20 + r.nextInt(31); // 20-50 seg
            acumulado += duracion;
            if (acumulado > t) break;
            String siguiente;
            do {
                siguiente = DIRECCIONES[r.nextInt(8)];
            } while (siguiente.equals(actual));
            actual = siguiente;
        }
        return actual;
    }

    /**
     * ¿Falta poco para que cambie el viento? (alerta ALR de cambio de viento se
     * dispara 5 seg antes según manual sección 4.1)
     */
    public static boolean vientoCambiaPronto(LocalDateTime inicio) {
        if (inicio == null) return false;
        long t = segundosTranscurridos(inicio);
        long semilla = inicio.toLocalTime().toSecondOfDay();
        Random r = new Random(semilla);
        long acumulado = 0;
        while (acumulado <= t + 5) {
            long duracion = 20 + r.nextInt(31);
            acumulado += duracion;
            if (acumulado > t && acumulado <= t + 5) return true;
            r.nextInt(8); // consumir el random de la dirección
        }
        return false;
    }

    /**
     * ¿Hay un pico de energía activo ahora?
     * Picos cada 30-90 seg, duran 15-25 seg, suman +150 kW.
     * Arrancan recién cuando la subestación está activa por primera vez.
     */
    public static boolean picoActivo(LocalDateTime inicio, LocalDateTime subestacionDesde) {
        if (subestacionDesde == null) return false;
        long t = Duration.between(subestacionDesde, LocalDateTime.now()).getSeconds();
        if (t < 0) return false;
        long semilla = subestacionDesde.toLocalTime().toSecondOfDay() + 31;
        Random r = new Random(semilla);
        long acumulado = 0;
        while (acumulado <= t) {
            long gap = 30 + r.nextInt(61);        // 30-90 entre picos
            long duracion = 15 + r.nextInt(11);   // 15-25 dura
            long inicioP = acumulado + gap;
            long finP = inicioP + duracion;
            if (t >= inicioP && t < finP) return true;
            if (inicioP > t) return false;
            acumulado = finP;
        }
        return false;
    }

    /** ¿Falta poco para que arranque un pico? (alerta 5 seg antes) */
    public static boolean picoProximo(LocalDateTime subestacionDesde) {
        if (subestacionDesde == null) return false;
        long t = Duration.between(subestacionDesde, LocalDateTime.now()).getSeconds();
        if (t < 0) return false;
        long semilla = subestacionDesde.toLocalTime().toSecondOfDay() + 31;
        Random r = new Random(semilla);
        long acumulado = 0;
        while (acumulado <= t + 5) {
            long gap = 30 + r.nextInt(61);
            long duracion = 15 + r.nextInt(11);
            long inicioP = acumulado + gap;
            if (inicioP > t && inicioP <= t + 5) return true;
            if (inicioP > t + 5) return false;
            acumulado = inicioP + duracion;
        }
        return false;
    }

    /**
     * Eficiencia del aerogenerador según el desfasaje con el viento.
     * Manual sección 4.2.1.
     */
    public static int eficienciaAerogenerador(String dirViento, String dirAero) {
        int idxV = indexDireccion(dirViento);
        int idxA = indexDireccion(dirAero);
        int diff = Math.abs(idxV - idxA);
        if (diff > 4) diff = 8 - diff;
        switch (diff) {
            case 0: return 100;
            case 1: return 85;
            case 2: return 60;
            case 3: return 30;
            case 4: return 0;
            default: return 0;
        }
    }

    private static int indexDireccion(String d) {
        for (int i = 0; i < DIRECCIONES.length; i++) {
            if (DIRECCIONES[i].equals(d)) return i;
        }
        return 0;
    }

    /**
     * Energía generada en kW por segundo en este instante.
     * Solo si: hay transformador configurado, subestación activa, ML conectado.
     */
    public static int kwActuales(boolean txConfigurado, boolean subestacionOn, boolean mlConectado,
                                  int eficiencia, boolean picoActivo) {
        if (!txConfigurado || !subestacionOn || !mlConectado) return 0;
        int base = (int) Math.round(200 * (eficiencia / 100.0));
        if (picoActivo) base += 150;
        return base;
    }

    /**
     * % de carga del transformador. La carga sube con los kW generados,
     * comparados con la potencia nominal del transformador.
     * Simplificación: usamos potencia_mva como techo proporcional.
     */
    public static int porcentajeCarga(int kwActuales, int potenciaMva) {
        // potencia_mva * 1000 = kW nominales. Si kw >= nominales, carga 100%+
        int nominal = potenciaMva * 1000;
        if (nominal == 0) return 0;
        // Para que se sienta apretado, asumimos que el 100% se alcanza a ~10% del nominal
        // (los aerogeneradores no llegan ni cerca de saturar transformadores grandes en realidad)
        return Math.min(150, (kwActuales * 10) / (nominal / 100));
    }

    /**
     * Genera las alertas activas en este instante según el estado del juego.
     * Códigos numéricos al estilo del juego real (ALR-XX, ERR-XX).
     */
    public static List<Alerta> alertasActivas(EstadoPartida e) {
        List<Alerta> lista = new ArrayList<>();

        if (e.inicio == null) return lista;

        // ALR informativas
        if (vientoCambiaPronto(e.inicio)) {
            lista.add(new Alerta("ALR-13", "ALR", "Próximo cambio de dirección de viento"));
        }
        if (picoProximo(e.subestacionDesde)) {
            lista.add(new Alerta("ALR-27", "ALR", "Próximo pico de energía"));
        }
        if (e.cargaActual > 90) {
            lista.add(new Alerta("ALR-88", "ALR", "Carga del transformador mayor al 90%"));
        }
        if (!e.mlConectado) {
            lista.add(new Alerta("ALR-42", "ALR", "El Aerogenerador todavía no entrenó el modelo"));
        }

        // ERR
        if (e.txQuemado) {
            lista.add(new Alerta("ERR-04", "ERR", "Transformador quemado"));
        }
        if (!e.subestacionOn && e.txConfigurado) {
            lista.add(new Alerta("ERR-11", "ERR", "Subestación inactiva o con falla"));
        }
        if (!e.txConfigurado && e.modeloTxActivo != null) {
            lista.add(new Alerta("ERR-08", "ERR", "Transformador no configurado"));
        }

        return lista;
    }

    /** Estructura simple para pasar el estado a este calculador. */
    public static class EstadoPartida {
        public LocalDateTime inicio;
        public LocalDateTime subestacionDesde;
        public String modeloTxActivo;
        public boolean txConfigurado;
        public boolean subestacionOn;
        public boolean mlConectado;
        public boolean txQuemado;
        public int cargaActual;
    }

    /** Estructura simple para una alerta. */
    public static class Alerta {
        public final String codigo;
        public final String tipo;
        public final String motivo;
        public Alerta(String codigo, String tipo, String motivo) {
            this.codigo = codigo;
            this.tipo = tipo;
            this.motivo = motivo;
        }
    }
}