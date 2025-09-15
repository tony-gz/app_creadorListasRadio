package com.radio.utils;

import com.radio.models.Cancion;
import com.radio.models.InsercionEspecial;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Randomizador {
    private static final Random random = new Random();

    // Extensiones de audio compatibles con ZaraRadio
    private static final List<String> EXTENSIONES_AUDIO = Arrays.asList(
            ".mp3", ".wav", ".wma", ".ogg", ".aac"
    );

    /**
     * Obtiene una lista de archivos de audio de una carpeta de forma aleatoria
     */
    public static List<Cancion> obtenerCancionesAleatorias(String rutaCarpeta, int cantidad) {
        List<Cancion> canciones = new ArrayList<>();

        File carpeta = new File(rutaCarpeta);
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            return canciones;
        }

        File[] archivos = carpeta.listFiles(file ->
                file.isFile() && esArchivoAudio(file.getName())
        );

        if (archivos == null || archivos.length == 0) {
            return canciones;
        }

        // Convertir archivos a lista y mezclar
        List<File> listaArchivos = new ArrayList<>(Arrays.asList(archivos));
        Collections.shuffle(listaArchivos, random);

        // Tomar la cantidad solicitada (o todos si hay menos)
        int cantidadFinal = Math.min(cantidad, listaArchivos.size());

        for (int i = 0; i < cantidadFinal; i++) {
            File archivo = listaArchivos.get(i);
            Cancion cancion = crearCancionDesdeArchivo(archivo);
            canciones.add(cancion);
        }

        return canciones;
    }

    /**
     * Obtiene una canción aleatoria de una carpeta
     */
    public static Cancion obtenerCancionAleatoria(String rutaCarpeta) {
        List<Cancion> canciones = obtenerCancionesAleatorias(rutaCarpeta, 1);
        return canciones.isEmpty() ? null : canciones.get(0);
    }

    /**
     * Obtiene una inserción especial aleatoria de una carpeta específica
     */
    public static InsercionEspecial obtenerInsercionAleatoria(String rutaCarpeta,
                                                              InsercionEspecial.TipoInsercion tipo) {
        File carpeta = new File(rutaCarpeta);
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            return null;
        }

        File[] archivos = carpeta.listFiles(file ->
                file.isFile() && esArchivoAudio(file.getName())
        );

        if (archivos == null || archivos.length == 0) {
            return null;
        }

        // Seleccionar archivo aleatorio
        File archivoSeleccionado = archivos[random.nextInt(archivos.length)];

        return crearInsercionDesdeArchivo(archivoSeleccionado, tipo);
    }

    /**
     * Obtiene múltiples inserciones aleatorias de una carpeta
     */
    public static List<InsercionEspecial> obtenerInsercionesAleatorias(String rutaCarpeta,
                                                                       InsercionEspecial.TipoInsercion tipo,
                                                                       int cantidad) {
        List<InsercionEspecial> inserciones = new ArrayList<>();

        File carpeta = new File(rutaCarpeta);
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            return inserciones;
        }

        File[] archivos = carpeta.listFiles(file ->
                file.isFile() && esArchivoAudio(file.getName())
        );

        if (archivos == null || archivos.length == 0) {
            return inserciones;
        }

        List<File> listaArchivos = new ArrayList<>(Arrays.asList(archivos));
        Collections.shuffle(listaArchivos, random);

        int cantidadFinal = Math.min(cantidad, listaArchivos.size());

        for (int i = 0; i < cantidadFinal; i++) {
            File archivo = listaArchivos.get(i);
            InsercionEspecial insercion = crearInsercionDesdeArchivo(archivo, tipo);
            inserciones.add(insercion);
        }

        return inserciones;
    }

    /**
     * Mezcla una lista existente de canciones
     */
    public static void mezclarCanciones(List<Cancion> canciones) {
        Collections.shuffle(canciones, random);
    }

    /**
     * Selecciona elementos aleatorios de una lista sin modificar la original
     */
    public static <T> List<T> seleccionarAleatorios(List<T> lista, int cantidad) {
        List<T> copia = new ArrayList<>(lista);
        Collections.shuffle(copia, random);
        return copia.subList(0, Math.min(cantidad, copia.size()));
    }

    // Métodos auxiliares privados
    private static boolean esArchivoAudio(String nombreArchivo) {
        String extension = nombreArchivo.toLowerCase();
        return EXTENSIONES_AUDIO.stream().anyMatch(extension::endsWith);
    }

    private static Cancion crearCancionDesdeArchivo(File archivo) {
        Cancion cancion = new Cancion();

        // Extraer información básica del nombre del archivo
        String nombreSinExtension = archivo.getName().replaceFirst("[.][^.]+$", "");

        // Intentar separar artista y título si están separados por " - "
        if (nombreSinExtension.contains(" - ")) {
            String[] partes = nombreSinExtension.split(" - ", 2);
            cancion.setArtista(partes[0].trim());
            cancion.setTitulo(partes[1].trim());
        } else {
            cancion.setArtista("Artista Desconocido");
            cancion.setTitulo(nombreSinExtension);
        }

        cancion.setRutaArchivo(archivo.getAbsolutePath());
        cancion.setDuracion(Duration.ofMinutes(3).plusSeconds(30)); // Duración estimada por defecto

        return cancion;
    }

    private static InsercionEspecial crearInsercionDesdeArchivo(File archivo,
                                                                InsercionEspecial.TipoInsercion tipo) {
        InsercionEspecial insercion = new InsercionEspecial();

        String nombreSinExtension = archivo.getName().replaceFirst("[.][^.]+$", "");

        insercion.setNombre(nombreSinExtension);
        insercion.setRutaArchivo(archivo.getAbsolutePath());
        insercion.setTipo(tipo);

        // Duraciones estimadas según el tipo
        switch (tipo) {
            case HIMNO_NACIONAL:
            case HIMNO_GUERRERO:
                insercion.setDuracion(Duration.ofMinutes(1).plusSeconds(30));
                break;
            case POEMA:
                insercion.setDuracion(Duration.ofMinutes(2));
                break;
            case LOCUCION_HORA:
                insercion.setDuracion(Duration.ofSeconds(10));
                break;
            case IDENTIFICACION:
                insercion.setDuracion(Duration.ofSeconds(15));
                break;
            case PROMO:
                insercion.setDuracion(Duration.ofSeconds(30));
                break;
            default:
                insercion.setDuracion(Duration.ofSeconds(30));
        }

        return insercion;
    }
}