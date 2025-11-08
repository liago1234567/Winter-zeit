package com.mauliwinterevent;

public enum Cosmetic {
    SNOW_TRAIL,
    AURORA_HALO;

    public static Cosmetic from(String s) {
        try {
            return Cosmetic.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
