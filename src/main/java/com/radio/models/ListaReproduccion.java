package com.radio.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ListaReproduccion {
    private LocalDate fecha;
    private LocalTime horaInicio; // 7:00 AM
    private LocalTime horaFin;    // ~8:07 PM
    private List<BloqueHora> bloques;
    private List<InsercionEspecial> apertura;  // Himno Nacional, Himno Guerrero, Poema
    private List<InsercionEspecial> cierre;    // Poema, Himno Guerrero, Himno Nacional
    private String rutaArchivoLst;

    public ListaReproduccion() {
        this.bloques = new ArrayList<>();
        this.apertura = new ArrayList<>();
        this.cierre = new ArrayList<>();
        this.horaInicio = LocalTime.of(7, 0);  // 7:00 AM por defecto
        this.horaFin = LocalTime.of(20, 7);    // 8:07 PM por defecto (con tolerancia)
        this.fecha = LocalDate.now();
    }

    public ListaReproduccion(LocalDate fecha) {
        this();
        this.fecha = fecha;
    }

    // Método para inicializar los 13 bloques de hora estándar
    public void inicializarBloquesEstandar() {
        bloques.clear();
        for (int hora = 7; hora < 20; hora++) {
            LocalTime inicio = LocalTime.of(hora, 0);
            LocalTime fin = LocalTime.of(hora + 1, 0);
            BloqueHora bloque = new BloqueHora(inicio, fin, Genero.VARIADO, "");
            bloques.add(bloque);
        }
    }

    // Getters y Setters
    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public List<BloqueHora> getBloques() {
        return bloques;
    }

    public void setBloques(List<BloqueHora> bloques) {
        this.bloques = bloques;
    }

    public void agregarBloque(BloqueHora bloque) {
        this.bloques.add(bloque);
    }

    public List<InsercionEspecial> getApertura() {
        return apertura;
    }

    public void setApertura(List<InsercionEspecial> apertura) {
        this.apertura = apertura;
    }

    public List<InsercionEspecial> getCierre() {
        return cierre;
    }

    public void setCierre(List<InsercionEspecial> cierre) {
        this.cierre = cierre;
    }

    public String getRutaArchivoLst() {
        return rutaArchivoLst;
    }

    public void setRutaArchivoLst(String rutaArchivoLst) {
        this.rutaArchivoLst = rutaArchivoLst;
    }

    // Método utilitario para obtener el total de canciones en toda la lista
    public int getTotalCanciones() {
        return bloques.stream().mapToInt(bloque -> bloque.getCanciones().size()).sum();
    }

    // Método utilitario para obtener el total de inserciones especiales
    public int getTotalInserciones() {
        int inserciones = apertura.size() + cierre.size();
        inserciones += bloques.stream().mapToInt(bloque -> bloque.getInserciones().size()).sum();
        return inserciones;
    }

    @Override
    public String toString() {
        return String.format("Lista del %s: %d bloques, %d canciones totales",
                fecha, bloques.size(), getTotalCanciones());
    }
}