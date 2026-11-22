package com.stonkscalc.engine;

import java.util.Locale;

public enum AngleMode {
    RAD, DEG;

    public static AngleMode parse(Object value) {
        if (value == null) return RAD;
        return value.toString().toUpperCase(Locale.ROOT).startsWith("DEG") ? DEG : RAD;
    }

    double toRadians(double x) {
        return this == DEG ? Math.toRadians(x) : x;
    }

    double fromRadians(double x) {
        return this == DEG ? Math.toDegrees(x) : x;
    }
}
