package com.rutina.model;

/**
 * Un ejercicio ubicado en la grilla del mesociclo (bloque E1, E2… de un día).
 *
 * @param dia      día del microciclo, desde 1
 * @param posicion orden dentro del día, desde 1
 * @param pctBolsa % de la bolsa de su tipo ese día (solo aproximación compleja)
 */
public record Bloque(int dia, int posicion, long ejercicioId, double pctBolsa) {
}
