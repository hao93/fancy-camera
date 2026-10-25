package com.silverroom.camera;

/** Per-look restraint: color is primary; only textured looks retain noticeable grain. */
public final class FilmGrain {
    private FilmGrain(){}
    private static final float[] SCALE={
        .08f,.22f,.20f,.18f,.45f,.65f,                  // Original 0..5
        .20f,.25f,.20f,.12f,.12f,.12f,.12f,.15f,.30f,.30f,.12f,.30f,
        .12f,.12f,.30f,.22f,.24f,.55f,                // Leica / Ricoh
        .30f,.12f,.20f,.60f,.60f,.35f,                // Classic stocks
        .35f,.12f,.18f,.35f,.12f,                     // Cinema
        .12f,.12f,.10f,.20f,.10f,.20f,                // Modern
        .60f,.40f,.20f,.12f,.50f,                     // Eras / CCD
        .12f,.12f,.45f,.25f,.12f,                     // Special
        .08f,.08f,.08f,.08f,.08f,.12f,                // Hasselblad inspiration: very fine texture
        .18f,.30f,.30f,.30f,.25f,.12f,                // Added classic stocks
        .18f,.20f,.20f,.15f,.12f,.20f,.12f,.35f,.12f,.18f,.25f,.30f,.18f,.20f // Film palettes
    };
    public static float scale(int stock){return SCALE[FilmCatalog.safe(stock)];}
    public static float effective(int stock,float grain){return (Float.isFinite(grain)?Math.max(0,Math.min(1,grain)):0)*scale(stock);}
}
