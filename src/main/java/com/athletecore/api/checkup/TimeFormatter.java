package com.athletecore.api.checkup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad de presentación para convertir tiempos entre segundos decimales
 * (BigDecimal, hasta 3 decimales) y formato mm:ss.ms (o hh:mm:ss.ms si supera
 * 1 hora). La entidad nunca formatea: el formato es responsabilidad exclusiva
 * de la capa de presentación (DTOs).
 */
public final class TimeFormatter {

    private static final int SCALE = 3;
    private static final BigDecimal SECONDS_PER_MINUTE = BigDecimal.valueOf(60);
    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MILLIS_PER_HOUR = 3_600_000L;

    private static final Pattern FORMATTED_PATTERN = Pattern.compile(
            "^(?:(\\d+):)?([0-5]?\\d):([0-5]?\\d)\\.(\\d{1,3})$");

    private TimeFormatter() {
        // Utilidad estática: no se instancia.
    }

    /**
     * Convierte segundos decimales a String mm:ss.ms (o hh:mm:ss.ms si >= 3600s).
     * null -> null; valores negativos lanzan IllegalArgumentException.
     *
     * @param seconds Tiempo en segundos (no negativo)
     * @return Tiempo formateado o null si la entrada es null
     * @throws IllegalArgumentException si seconds es negativo
     */
    public static String toFormatted(BigDecimal seconds) {
        if (seconds == null) {
            return null;
        }
        if (seconds.signum() < 0) {
            throw new IllegalArgumentException("El tiempo no puede ser negativo: " + seconds);
        }
        long totalMillis = seconds.setScale(SCALE, RoundingMode.HALF_UP)
                .movePointRight(SCALE).longValueExact();
        long hours = totalMillis / MILLIS_PER_HOUR;
        long minutes = (totalMillis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE;
        long wholeSeconds = (totalMillis % MILLIS_PER_MINUTE) / 1000;
        long millis = totalMillis % 1000;
        String mmss = String.format("%02d:%02d.%03d", minutes, wholeSeconds, millis);
        return hours > 0 ? String.format("%02d:%s", hours, mmss) : mmss;
    }

    /**
     * Convierte mm:ss.ms (o hh:mm:ss.ms) a segundos decimales con 3 decimales.
     * Los dígitos tras el punto se interpretan como milisegundos (p.ej. "25" -> 0.025).
     * null o vacío -> null; formato no válido lanza IllegalArgumentException.
     *
     * @param formatted Tiempo en formato mm:ss.ms
     * @return Segundos decimales o null si la entrada es null/vacía
     * @throws IllegalArgumentException si el formato no es válido
     */
    public static BigDecimal toSeconds(String formatted) {
        if (formatted == null || formatted.isBlank()) {
            return null;
        }
        Matcher matcher = FORMATTED_PATTERN.matcher(formatted.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Formato de tiempo inválido (se espera mm:ss.ms): " + formatted);
        }
        long hours = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0L;
        long minutes = Long.parseLong(matcher.group(2));
        long seconds = Long.parseLong(matcher.group(3));
        BigDecimal millis = BigDecimal.valueOf(Long.parseLong(matcher.group(4)), SCALE);
        BigDecimal total = BigDecimal.valueOf(hours).multiply(SECONDS_PER_HOUR)
                .add(BigDecimal.valueOf(minutes).multiply(SECONDS_PER_MINUTE))
                .add(BigDecimal.valueOf(seconds))
                .add(millis);
        return total.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Formatea un delta (diferencia de tiempos) con signo explícito,
     * p.ej. "-00:02.000" o "+00:02.000". null -> null; cero -> "00:00.000".
     *
     * @param seconds Diferencia en segundos (puede ser negativa)
     * @return Delta formateado con signo o null si la entrada es null
     */
    public static String toSignedFormatted(BigDecimal seconds) {
        if (seconds == null) {
            return null;
        }
        int signum = seconds.signum();
        if (signum == 0) {
            return "00:00.000";
        }
        return signum < 0
                ? "-" + toFormatted(seconds.negate())
                : "+" + toFormatted(seconds);
    }
}
