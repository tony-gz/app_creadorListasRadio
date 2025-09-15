package com.radio.services;

import com.radio.models.*;
import com.radio.utils.Randomizador;
import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class GeneradorListaService {
    private Set<String> cancionesUsadas = new HashSet<>();
    // Configuraciones por defecto
    private static final int CANCIONES_POR_HORA = 15; // Aproximadamente para llenar 1 hora
    private static final int CADA_N_CANCIONES_LOCUCION = 3; // Locución cada 3 canciones
    private static final int TOLERANCIA_MINUTOS_DEFAULT = 5;

    // Rutas de carpetas especiales
    private String rutaElementosEspeciales;
    private String rutaIdentificaciones;
    private String rutaFelicitaciones;  // NUEVO
    private String rutaPromosA;         // NUEVO
    private String rutaPromosB;         // NUEVO

    // Controladores de rotación para cada tipo de elemento
    private RotadorElementos rotadorIdentificaciones;
    private RotadorElementos rotadorFelicitaciones;
    private RotadorElementos rotadorPromosA;
    private RotadorElementos rotadorPromosB;

    public GeneradorListaService() {
        // Los rotadores se inicializarán cuando se configuren las rutas
    }

    /**
     * Genera una lista completa de reproducción aplicando todas las reglas
     */
    // Agregar esta variable de instancia al inicio de la clase


    // Modificar el método generarListaCompleta para incluir verificación
    public ListaReproduccion generarListaCompleta(LocalDate fecha, List<ConfiguracionBloque> configuracion) {
        ListaReproduccion lista = new ListaReproduccion(fecha);

        // NUEVO: Limpiar set de canciones usadas al inicio
        cancionesUsadas.clear();

        inicializarRotadores();
        configurarBloques(lista, configuracion);
        generarApertura(lista);

        for (BloqueHora bloque : lista.getBloques()) {
            generarContenidoBloqueConVerificacion(bloque); // CAMBIADO: Usar método con verificación
        }

        insertarElementosEspeciales(lista);
        generarCierre(lista);

        return lista;
    }

    // NUEVO: Método que genera contenido verificando que no se repitan canciones
    private void generarContenidoBloqueConVerificacion(BloqueHora bloque) {
        if (bloque.getRutaCarpeta() == null || bloque.getRutaCarpeta().isEmpty()) {
            return;
        }

        List<Cancion> cancionesBloque = new ArrayList<>();
        List<String> archivosDisponibles = ArchivoService.obtenerArchivosAudio(bloque.getRutaCarpeta());

        // Mezclar archivos disponibles
        Collections.shuffle(archivosDisponibles);

        int cancionesNecesarias = CANCIONES_POR_HORA;
        int intentos = 0;
        int maxIntentos = archivosDisponibles.size() * 2; // Límite para evitar bucle infinito

        for (String rutaArchivo : archivosDisponibles) {
            if (cancionesBloque.size() >= cancionesNecesarias) {
                break;
            }

            if (intentos++ > maxIntentos) {
                System.out.println("Advertencia: No se pudieron encontrar suficientes canciones únicas para el bloque " +
                        bloque.getHoraInicio() + "-" + bloque.getHoraFin());
                break;
            }

            // Verificar si la canción ya fue usada
            if (!cancionesUsadas.contains(rutaArchivo)) {
                Cancion cancion = crearCancionDesdeRuta(rutaArchivo, bloque.getGenero());
                cancionesBloque.add(cancion);
                cancionesUsadas.add(rutaArchivo);
            }
        }

        bloque.setCanciones(cancionesBloque);

        System.out.println("Bloque " + bloque.getHoraInicio() + "-" + bloque.getHoraFin() +
                ": " + cancionesBloque.size() + " canciones únicas agregadas. " +
                "Total canciones usadas en la lista: " + cancionesUsadas.size());
    }

    // NUEVO: Método auxiliar para crear canción desde ruta
    private Cancion crearCancionDesdeRuta(String rutaArchivo, Genero genero) {
        File archivo = new File(rutaArchivo);
        String nombreSinExtension = archivo.getName().replaceFirst("[.][^.]+$", "");

        Cancion cancion = new Cancion();

        if (nombreSinExtension.contains(" - ")) {
            String[] partes = nombreSinExtension.split(" - ", 2);
            cancion.setArtista(partes[0].trim());
            cancion.setTitulo(partes[1].trim());
        } else {
            cancion.setArtista("Artista Desconocido");
            cancion.setTitulo(nombreSinExtension);
        }

        cancion.setRutaArchivo(rutaArchivo);
        cancion.setGenero(genero);
        cancion.setDuracion(Duration.ofMinutes(3).plusSeconds(30));

        return cancion;
    }

    /**
     * Inicializa los rotadores para cada tipo de elemento especial
     */
    private void inicializarRotadores() {
        if (rutaIdentificaciones != null && !rutaIdentificaciones.isEmpty()) {
            rotadorIdentificaciones = new RotadorElementos(rutaIdentificaciones, "identificacion");
        }

        if (rutaFelicitaciones != null && !rutaFelicitaciones.isEmpty()) {
            rotadorFelicitaciones = new RotadorElementos(rutaFelicitaciones, "felicitacion");
        }

        if (rutaPromosA != null && !rutaPromosA.isEmpty()) {
            rotadorPromosA = new RotadorElementos(rutaPromosA, "promoA");
        }

        if (rutaPromosB != null && !rutaPromosB.isEmpty()) {
            rotadorPromosB = new RotadorElementos(rutaPromosB, "promoB");
        }
    }

    /**
     * Configura los bloques de hora según la configuración proporcionada
     */
    private void configurarBloques(ListaReproduccion lista, List<ConfiguracionBloque> configuracion) {
        lista.getBloques().clear();

        for (ConfiguracionBloque config : configuracion) {
            BloqueHora bloque = new BloqueHora(
                    config.getHoraInicio(),
                    config.getHoraFin(),
                    config.getGenero(),
                    config.getRutaCarpeta()
            );
            bloque.setToleranciaMinutos(TOLERANCIA_MINUTOS_DEFAULT);
            lista.agregarBloque(bloque);
        }
    }

    /**
     * Genera el contenido de canciones para un bloque específico
     */
    private void generarContenidoBloque(BloqueHora bloque) {
        if (bloque.getRutaCarpeta() == null || bloque.getRutaCarpeta().isEmpty()) {
            return;
        }

        // Obtener canciones aleatorias de la carpeta del género
        List<Cancion> canciones = Randomizador.obtenerCancionesAleatorias(
                bloque.getRutaCarpeta(),
                CANCIONES_POR_HORA
        );

        // Asignar género a las canciones
        for (Cancion cancion : canciones) {
            cancion.setGenero(bloque.getGenero());
        }

        bloque.setCanciones(canciones);
    }

    /**
     * Genera la secuencia de apertura
     */
    private void generarApertura(ListaReproduccion lista) {
        List<InsercionEspecial> apertura = new ArrayList<>();

        // 1. Himno Nacional (archivos que empiecen con "01")
        InsercionEspecial himnoNacional = obtenerElementoPorIdentificador("01", InsercionEspecial.TipoInsercion.HIMNO_NACIONAL);
        if (himnoNacional != null) apertura.add(himnoNacional);

        // 2. Himno de Guerrero (archivos que empiecen con "02")
        InsercionEspecial himnoGuerrero = obtenerElementoPorIdentificador("02", InsercionEspecial.TipoInsercion.HIMNO_GUERRERO);
        if (himnoGuerrero != null) apertura.add(himnoGuerrero);

        // 3. Poema (archivos que empiecen con "03")
        InsercionEspecial poema = obtenerElementoPorIdentificador("03", InsercionEspecial.TipoInsercion.POEMA);
        if (poema != null) apertura.add(poema);

        lista.setApertura(apertura);
    }

    /**
     * Genera la secuencia de cierre (orden inverso a la apertura)
     */
    private void generarCierre(ListaReproduccion lista) {
        List<InsercionEspecial> cierre = new ArrayList<>();

        // Orden inverso: Poema, Himno Guerrero, Himno Nacional
        InsercionEspecial poema = obtenerElementoPorIdentificador("03", InsercionEspecial.TipoInsercion.POEMA);
        if (poema != null) cierre.add(poema);

        InsercionEspecial himnoGuerrero = obtenerElementoPorIdentificador("02", InsercionEspecial.TipoInsercion.HIMNO_GUERRERO);
        if (himnoGuerrero != null) cierre.add(himnoGuerrero);

        InsercionEspecial himnoNacional = obtenerElementoPorIdentificador("01", InsercionEspecial.TipoInsercion.HIMNO_NACIONAL);
        if (himnoNacional != null) cierre.add(himnoNacional);

        lista.setCierre(cierre);
    }

    /**
     * Inserta elementos especiales según el patrón especificado:
     * Patrón: locucion_hora -> identificacion + felicitacion
     *         locucion_hora -> promoA + promoB
     *         locucion_hora -> promoA + promoB
     *         (se reinicia el ciclo)
     */
    private void insertarElementosEspeciales(ListaReproduccion lista) {
        int contadorLocuciones = 0;

        for (BloqueHora bloque : lista.getBloques()) {
            List<Cancion> cancionesOriginales = new ArrayList<>(bloque.getCanciones());
            List<Cancion> cancionesConInserciones = new ArrayList<>();

            for (int i = 0; i < cancionesOriginales.size(); i++) {
                cancionesConInserciones.add(cancionesOriginales.get(i));

                // Insertar elementos cada 3 canciones
                if ((i + 1) % CADA_N_CANCIONES_LOCUCION == 0) {
                    // Insertar marcador de tiempo
                    Cancion marcadorTiempo = new Cancion();
                    marcadorTiempo.setTitulo("TIME_MARKER");
                    marcadorTiempo.setArtista("TIME_MARKER");
                    marcadorTiempo.setRutaArchivo("-1 .time");
                    cancionesConInserciones.add(marcadorTiempo);

                    // Obtener elementos según el patrón
                    List<InsercionEspecial> elementos = obtenerElementosSegunPatron(contadorLocuciones);

                    // Convertir inserciones a canciones y añadirlas
                    for (InsercionEspecial elemento : elementos) {
                        if (elemento != null) {
                            Cancion cancionEspecial = convertirInsercionACancion(elemento, false);
                            cancionesConInserciones.add(cancionEspecial);
                        }
                    }

                    contadorLocuciones++;
                }
            }

            bloque.setCanciones(cancionesConInserciones);
            bloque.setInserciones(new ArrayList<>());
        }
    }

    /**
     * Obtiene los elementos según el patrón especificado
     * CORREGIDO: El ciclo es de 3 pasos, no 4
     */
    private List<InsercionEspecial> obtenerElementosSegunPatron(int numeroLocucion) {
        List<InsercionEspecial> elementos = new ArrayList<>();

        int posicionEnCiclo = numeroLocucion % 3; // CORREGIDO: Ciclo de 3, no 4

        switch (posicionEnCiclo) {
            case 0: // Identificación + Felicitación
                if (rotadorIdentificaciones != null) {
                    InsercionEspecial identificacion = rotadorIdentificaciones.obtenerSiguiente();
                    if (identificacion != null) elementos.add(identificacion);
                }
                if (rotadorFelicitaciones != null) {
                    InsercionEspecial felicitacion = rotadorFelicitaciones.obtenerSiguiente();
                    if (felicitacion != null) elementos.add(felicitacion);
                }
                break;

            case 1: // PromoA + PromoB (primera vez)
            case 2: // PromoA + PromoB (segunda vez)
                if (rotadorPromosA != null) {
                    InsercionEspecial promoA = rotadorPromosA.obtenerSiguiente();
                    if (promoA != null) elementos.add(promoA);
                }
                if (rotadorPromosB != null) {
                    InsercionEspecial promoB = rotadorPromosB.obtenerSiguiente();
                    if (promoB != null) elementos.add(promoB);
                }
                break;
        }

        return elementos;
    }

    /**
     * Obtiene un elemento por identificador de la carpeta de elementos especiales
     */
    private InsercionEspecial obtenerElementoPorIdentificador(String identificador, InsercionEspecial.TipoInsercion tipo) {
        File carpeta = new File(rutaElementosEspeciales);
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            return null;
        }

        File[] archivos = carpeta.listFiles(file ->
                file.isFile() &&
                        file.getName().startsWith(identificador) &&
                        esArchivoAudio(file.getName())
        );

        if (archivos == null || archivos.length == 0) {
            return null;
        }

        // Seleccionar archivo aleatorio si hay varios con el mismo identificador
        File archivoSeleccionado = archivos[new Random().nextInt(archivos.length)];
        return crearInsercionDesdeArchivo(archivoSeleccionado, tipo);
    }

    /**
     * Verifica si un archivo es de audio
     */
    private boolean esArchivoAudio(String nombreArchivo) {
        String extension = nombreArchivo.toLowerCase();
        return extension.endsWith(".mp3") || extension.endsWith(".wav") ||
                extension.endsWith(".wma") || extension.endsWith(".ogg") ||
                extension.endsWith(".aac");
    }

    /**
     * Crea una inserción especial desde un archivo
     */
    private InsercionEspecial crearInsercionDesdeArchivo(File archivo, InsercionEspecial.TipoInsercion tipo) {
        InsercionEspecial insercion = new InsercionEspecial();
        String nombreSinExtension = archivo.getName().replaceFirst("[.][^.]+$", "");

        insercion.setNombre(nombreSinExtension);
        insercion.setRutaArchivo(archivo.getAbsolutePath());
        insercion.setTipo(tipo);

        switch (tipo) {
            case HIMNO_NACIONAL:
            case HIMNO_GUERRERO:
                insercion.setDuracion(Duration.ofMinutes(1).plusSeconds(30));
                break;
            case POEMA:
                insercion.setDuracion(Duration.ofMinutes(2));
                break;
            default:
                insercion.setDuracion(Duration.ofSeconds(30));
        }

        return insercion;
    }

    /**
     * Convierte una inserción especial en una canción para poder insertarla en la secuencia
     */
    private Cancion convertirInsercionACancion(InsercionEspecial insercion, boolean esLocucionHora) {
        Cancion cancion = new Cancion();
        cancion.setTitulo(insercion.getNombre());
        cancion.setArtista(insercion.getTipo().getNombre());
        cancion.setRutaArchivo(insercion.getRutaArchivo()); // Esta debe ser la ruta completa del archivo
        cancion.setDuracion(insercion.getDuracion());
        cancion.setGenero(Genero.VARIADO);
        return cancion;
    }

    /**
     * Crea una configuración estándar de 13 horas (7:00 AM - 8:00 PM)
     */
    public List<ConfiguracionBloque> crearConfiguracionEstandar() {
        List<ConfiguracionBloque> configuracion = new ArrayList<>();

        for (int hora = 7; hora < 20; hora++) {
            ConfiguracionBloque config = new ConfiguracionBloque(
                    LocalTime.of(hora, 0),
                    LocalTime.of(hora + 1, 0),
                    Genero.VARIADO,
                    ""
            );
            configuracion.add(config);
        }

        return configuracion;
    }

    // Getters y Setters para rutas de carpetas especiales
    public String getRutaElementosEspeciales() { return rutaElementosEspeciales; }
    public void setRutaElementosEspeciales(String rutaElementosEspeciales) {
        this.rutaElementosEspeciales = rutaElementosEspeciales;
    }

    public String getRutaIdentificaciones() { return rutaIdentificaciones; }
    public void setRutaIdentificaciones(String rutaIdentificaciones) {
        this.rutaIdentificaciones = rutaIdentificaciones;
    }

    public String getRutaFelicitaciones() { return rutaFelicitaciones; }
    public void setRutaFelicitaciones(String rutaFelicitaciones) {
        this.rutaFelicitaciones = rutaFelicitaciones;
    }

    public String getRutaPromosA() { return rutaPromosA; }
    public void setRutaPromosA(String rutaPromosA) {
        this.rutaPromosA = rutaPromosA;
    }

    public String getRutaPromosB() { return rutaPromosB; }
    public void setRutaPromosB(String rutaPromosB) {
        this.rutaPromosB = rutaPromosB;
    }

    // Clase auxiliar para configuración de bloques
    public static class ConfiguracionBloque {
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private Genero genero;
        private String rutaCarpeta;

        public ConfiguracionBloque(LocalTime horaInicio, LocalTime horaFin, Genero genero, String rutaCarpeta) {
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
            this.genero = genero;
            this.rutaCarpeta = rutaCarpeta;
        }

        // Getters y Setters
        public LocalTime getHoraInicio() { return horaInicio; }
        public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

        public LocalTime getHoraFin() { return horaFin; }
        public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

        public Genero getGenero() { return genero; }
        public void setGenero(Genero genero) { this.genero = genero; }

        public String getRutaCarpeta() { return rutaCarpeta; }
        public void setRutaCarpeta(String rutaCarpeta) { this.rutaCarpeta = rutaCarpeta; }
    }

    /**
     * Clase interna para manejar la rotación de elementos especiales
     * COMPLETAMENTE CORREGIDA para manejar correctamente la secuencia numerada
     */
    private static class RotadorElementos {
        private String rutaCarpeta;
        private String prefijoArchivo;
        private int indiceActual;
        private List<File> archivosDisponibles; // CAMBIADO: Usar archivos directamente
        private Random random = new Random();
        private boolean inicializado = false;

        public RotadorElementos(String rutaCarpeta, String prefijoArchivo) {
            this.rutaCarpeta = rutaCarpeta;
            this.prefijoArchivo = prefijoArchivo;
            this.archivosDisponibles = new ArrayList<>();
            inicializar();
        }

        private void inicializar() {
            File carpeta = new File(rutaCarpeta);
            if (!carpeta.exists() || !carpeta.isDirectory()) {
                System.out.println("Carpeta no existe: " + rutaCarpeta);
                return;
            }

            // Buscar TODOS los archivos de audio y ordenarlos
            File[] archivos = carpeta.listFiles(file -> {
                return file.isFile() && esArchivoAudio(file.getName());
            });

            if (archivos != null && archivos.length > 0) {
                // Ordenar por nombre de archivo para mantener secuencia numérica
                java.util.Arrays.sort(archivos, (a, b) -> {
                    String numA = extraerNumeroDeArchivo(a.getName());
                    String numB = extraerNumeroDeArchivo(b.getName());
                    if (numA != null && numB != null) {
                        try {
                            return Integer.compare(Integer.parseInt(numA), Integer.parseInt(numB));
                        } catch (NumberFormatException e) {
                            return a.getName().compareTo(b.getName());
                        }
                    }
                    return a.getName().compareTo(b.getName());
                });

                // Agregar todos los archivos a la lista
                for (File archivo : archivos) {
                    archivosDisponibles.add(archivo);
                }

                if (!archivosDisponibles.isEmpty()) {
                    // EMPEZAR DESDE UN ÍNDICE ALEATORIO
                    indiceActual = random.nextInt(archivosDisponibles.size());
                    inicializado = true;

                    System.out.println("Rotador " + prefijoArchivo + " inicializado:");
                    System.out.println("  - Total archivos: " + archivosDisponibles.size());
                    System.out.println("  - Índice inicial aleatorio: " + indiceActual);
                    System.out.println("  - Archivo inicial: " + archivosDisponibles.get(indiceActual).getName());
                }
            } else {
                System.out.println("No se encontraron archivos de audio en: " + rutaCarpeta);
            }
        }

        private String extraerNumeroDeArchivo(String nombreArchivo) {
            // Buscar número de 2-3 dígitos al inicio
            java.util.regex.Pattern patron = java.util.regex.Pattern.compile("^(\\d{2,3})");
            java.util.regex.Matcher matcher = patron.matcher(nombreArchivo);

            if (matcher.find()) {
                return matcher.group(1);
            }

            // Buscar cualquier número de 2-3 dígitos
            patron = java.util.regex.Pattern.compile("(\\d{2,3})");
            matcher = patron.matcher(nombreArchivo);

            if (matcher.find()) {
                return matcher.group(1);
            }

            return "000"; // Valor por defecto
        }

        private boolean esArchivoAudio(String nombreArchivo) {
            String extension = nombreArchivo.toLowerCase();
            return extension.endsWith(".mp3") || extension.endsWith(".wav") ||
                    extension.endsWith(".wma") || extension.endsWith(".ogg") ||
                    extension.endsWith(".aac");
        }

        public InsercionEspecial obtenerSiguiente() {
            if (!inicializado || archivosDisponibles.isEmpty()) {
                System.out.println("Rotador " + prefijoArchivo + " no inicializado o sin elementos");
                return null;
            }

            // Obtener archivo actual
            File archivoActual = archivosDisponibles.get(indiceActual);
            InsercionEspecial elemento = crearInsercionDesdeArchivo(archivoActual);

            System.out.println("Rotador " + prefijoArchivo + " seleccionó:");
            System.out.println("  - Índice: " + indiceActual + "/" + archivosDisponibles.size());
            System.out.println("  - Archivo: " + archivoActual.getName());
            System.out.println("  - Ruta: " + archivoActual.getAbsolutePath());

            // AVANZAR AL SIGUIENTE ÍNDICE DE FORMA SECUENCIAL
            indiceActual = (indiceActual + 1) % archivosDisponibles.size();

            System.out.println("  - Próximo índice: " + indiceActual);

            return elemento;
        }

        private InsercionEspecial crearInsercionDesdeArchivo(File archivo) {
            InsercionEspecial insercion = new InsercionEspecial();
            String nombreSinExtension = archivo.getName().replaceFirst("[.][^.]+$", "");

            insercion.setNombre(nombreSinExtension);
            insercion.setRutaArchivo(archivo.getAbsolutePath());
            insercion.setDuracion(Duration.ofSeconds(30));

            switch (prefijoArchivo.toLowerCase()) {
                case "identificacion":
                    insercion.setTipo(InsercionEspecial.TipoInsercion.IDENTIFICACION);
                    insercion.setDuracion(Duration.ofSeconds(15));
                    break;
                case "felicitacion":
                    insercion.setTipo(InsercionEspecial.TipoInsercion.IDENTIFICACION);
                    insercion.setDuracion(Duration.ofSeconds(20));
                    break;
                case "promoa":
                case "promob":
                    insercion.setTipo(InsercionEspecial.TipoInsercion.PROMO);
                    insercion.setDuracion(Duration.ofSeconds(30));
                    break;
                default:
                    insercion.setTipo(InsercionEspecial.TipoInsercion.PROMO);
            }

            return insercion;
        }
    }
}