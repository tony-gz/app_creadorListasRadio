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
    private String rutaFelicitaciones;
    private String rutaPromosA;
    private String rutaPromosB;

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
    public ListaReproduccion generarListaCompleta(LocalDate fecha, List<ConfiguracionBloque> configuracion) {
        ListaReproduccion lista = new ListaReproduccion(fecha);

        // Limpiar set de canciones usadas al inicio
        cancionesUsadas.clear();

        inicializarRotadores();
        configurarBloques(lista, configuracion);
        generarApertura(lista);

        // Generar contenido por bloque
        for (BloqueHora bloque : lista.getBloques()) {
            generarContenidoBloqueConVerificacion(bloque);
        }

        // CORREGIDO: Insertar elementos especiales después de generar las canciones
        insertarElementosEspecialesCorregido(lista);
        generarCierre(lista);

        return lista;
    }

    /**
     * NUEVO: Método corregido para insertar elementos especiales
     * Funciona bloque por bloque, insertando cada 3 canciones según el patrón
     */
    private void insertarElementosEspecialesCorregido(ListaReproduccion lista) {
        int contadorLocucionesGlobal = 0;

        for (BloqueHora bloque : lista.getBloques()) {
            List<Cancion> cancionesOriginales = new ArrayList<>(bloque.getCanciones());
            List<Cancion> cancionesConInserciones = new ArrayList<>();

            System.out.println("\nProcesando bloque: " + bloque.getHoraInicio() + "-" + bloque.getHoraFin());
            System.out.println("Canciones originales: " + cancionesOriginales.size());

            for (int i = 0; i < cancionesOriginales.size(); i++) {
                // Agregar la canción
                cancionesConInserciones.add(cancionesOriginales.get(i));

                // Verificar si es momento de insertar locución (cada 3 canciones)
                if ((i + 1) % CADA_N_CANCIONES_LOCUCION == 0) {
                    System.out.println("Insertando locución después de canción " + (i + 1));

                    // Insertar marcador de tiempo
                    Cancion marcadorTiempo = crearMarcadorTiempo();
                    cancionesConInserciones.add(marcadorTiempo);

                    // Obtener elementos según el patrón
                    List<InsercionEspecial> elementos = obtenerElementosSegunPatronCorregido(contadorLocucionesGlobal);

                    // Convertir inserciones a canciones y añadirlas
                    for (InsercionEspecial elemento : elementos) {
                        if (elemento != null) {
                            Cancion cancionEspecial = convertirInsercionACancion(elemento);
                            cancionesConInserciones.add(cancionEspecial);
                            System.out.println("  - Insertado: " + elemento.getNombre() + " (" + elemento.getTipo().getNombre() + ")");
                        }
                    }

                    contadorLocucionesGlobal++;
                    System.out.println("Contador global de locuciones: " + contadorLocucionesGlobal);
                }
            }

            // Actualizar las canciones del bloque con las inserciones
            bloque.setCanciones(cancionesConInserciones);
            bloque.setInserciones(new ArrayList<>()); // Limpiar inserciones separadas

            System.out.println("Bloque finalizado con " + cancionesConInserciones.size() + " elementos totales");
        }
    }

    /**
     * NUEVO: Crea marcador de tiempo
     */
    private Cancion crearMarcadorTiempo() {
        Cancion marcadorTiempo = new Cancion();
        marcadorTiempo.setTitulo("TIME_MARKER");
        marcadorTiempo.setArtista("TIME_MARKER");
        marcadorTiempo.setRutaArchivo(".time");
        marcadorTiempo.setDuracion(Duration.ofSeconds(1));
        marcadorTiempo.setGenero(Genero.VARIADO);
        return marcadorTiempo;
    }

    /**
     * CORREGIDO: Obtiene los elementos según el patrón especificado
     * Patrón:
     * Locución 0: Identificación + Felicitación
     * Locución 1: PromoA + PromoB
     * Locución 2: PromoA + PromoB
     * (se reinicia el ciclo)
     */
    private List<InsercionEspecial> obtenerElementosSegunPatronCorregido(int numeroLocucion) {
        List<InsercionEspecial> elementos = new ArrayList<>();
        int posicionEnCiclo = numeroLocucion % 3; // Ciclo de 3

        System.out.println("  Locución " + numeroLocucion + ", posición en ciclo: " + posicionEnCiclo);

        switch (posicionEnCiclo) {
            case 0: // Identificación + Felicitación
                System.out.println("  Patrón: Identificación + Felicitación");
                if (rotadorIdentificaciones != null) {
                    InsercionEspecial identificacion = rotadorIdentificaciones.obtenerSiguiente();
                    if (identificacion != null) {
                        elementos.add(identificacion);
                        System.out.println("    - Identificación: " + identificacion.getNombre());
                    }
                }
                if (rotadorFelicitaciones != null) {
                    InsercionEspecial felicitacion = rotadorFelicitaciones.obtenerSiguiente();
                    if (felicitacion != null) {
                        elementos.add(felicitacion);
                        System.out.println("    - Felicitación: " + felicitacion.getNombre());
                    }
                }
                break;

            case 1: // PromoA + PromoB (primera vez)
                System.out.println("  Patrón: PromoA + PromoB (primera vez)");
                if (rotadorPromosA != null) {
                    InsercionEspecial promoA = rotadorPromosA.obtenerSiguiente();
                    if (promoA != null) {
                        elementos.add(promoA);
                        System.out.println("    - PromoA: " + promoA.getNombre());
                    }
                }
                if (rotadorPromosB != null) {
                    InsercionEspecial promoB = rotadorPromosB.obtenerSiguiente();
                    if (promoB != null) {
                        elementos.add(promoB);
                        System.out.println("    - PromoB: " + promoB.getNombre());
                    }
                }
                break;

            case 2: // PromoA + PromoB (segunda vez)
                System.out.println("  Patrón: PromoA + PromoB (segunda vez)");
                if (rotadorPromosA != null) {
                    InsercionEspecial promoA = rotadorPromosA.obtenerSiguiente();
                    if (promoA != null) {
                        elementos.add(promoA);
                        System.out.println("    - PromoA: " + promoA.getNombre());
                    }
                }
                if (rotadorPromosB != null) {
                    InsercionEspecial promoB = rotadorPromosB.obtenerSiguiente();
                    if (promoB != null) {
                        elementos.add(promoB);
                        System.out.println("    - PromoB: " + promoB.getNombre());
                    }
                }
                break;
        }

        return elementos;
    }

    /**
     * Método que genera contenido verificando que no se repitan canciones
     */
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
        int maxIntentos = archivosDisponibles.size() * 2;

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

    /**
     * Método auxiliar para crear canción desde ruta
     */
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
    // Reemplazar el método inicializarRotadores() en GeneradorListaService.java
    private void inicializarRotadores() {
        System.out.println("Inicializando rotadores...");

        if (rutaIdentificaciones != null && !rutaIdentificaciones.isEmpty()) {
            System.out.println("Inicializando rotador de identificaciones: " + rutaIdentificaciones);
            rotadorIdentificaciones = new RotadorElementos(rutaIdentificaciones, "identificacion");
        } else {
            System.out.println("ADVERTENCIA: Ruta de identificaciones no configurada");
        }

        if (rutaFelicitaciones != null && !rutaFelicitaciones.isEmpty()) {
            System.out.println("Inicializando rotador de felicitaciones: " + rutaFelicitaciones);
            rotadorFelicitaciones = new RotadorElementos(rutaFelicitaciones, "felicitacion");
        } else {
            System.out.println("ADVERTENCIA: Ruta de felicitaciones no configurada");
        }

        if (rutaPromosA != null && !rutaPromosA.isEmpty()) {
            System.out.println("Inicializando rotador de promosA: " + rutaPromosA);
            rotadorPromosA = new RotadorElementos(rutaPromosA, "promoa");
        } else {
            System.out.println("ADVERTENCIA: Ruta de promosA no configurada");
        }

        if (rutaPromosB != null && !rutaPromosB.isEmpty()) {
            System.out.println("Inicializando rotador de promosB: " + rutaPromosB);
            rotadorPromosB = new RotadorElementos(rutaPromosB, "promob");
        } else {
            System.out.println("ADVERTENCIA: Ruta de promosB no configurada");
        }

        System.out.println("Rotadores inicializados.");
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
    private Cancion convertirInsercionACancion(InsercionEspecial insercion) {
        Cancion cancion = new Cancion();
        cancion.setTitulo(insercion.getNombre());
        cancion.setArtista(insercion.getTipo().getNombre());
        cancion.setRutaArchivo(insercion.getRutaArchivo());
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
     */
    /**
     * Clase interna CORREGIDA para manejar la rotación de elementos especiales
     * Maneja secuencia numérica correcta: inicio aleatorio, luego secuencial
     */
    private static class RotadorElementos {
        private String rutaCarpeta;
        private String prefijoArchivo;
        private int indiceActual;
        private List<File> archivosDisponibles;
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

            // Buscar TODOS los archivos de audio numerados
            File[] archivos = carpeta.listFiles(file -> {
                return file.isFile() &&
                        esArchivoAudio(file.getName()) &&
                        tieneNumeroAlInicio(file.getName());
            });

            if (archivos != null && archivos.length > 0) {
                System.out.println("Rotador " + prefijoArchivo + " - Archivos encontrados en " + rutaCarpeta + ":");
                for (File archivo : archivos) {
                    System.out.println("  - " + archivo.getName() + " (número: " + extraerNumeroDeArchivo(archivo.getName()) + ")");
                }

                // CORREGIDO: Ordenar por número extraído, no por nombre
                java.util.Arrays.sort(archivos, (a, b) -> {
                    int numA = Integer.parseInt(extraerNumeroDeArchivo(a.getName()));
                    int numB = Integer.parseInt(extraerNumeroDeArchivo(b.getName()));
                    return Integer.compare(numA, numB);
                });

                // Agregar archivos ordenados a la lista
                for (File archivo : archivos) {
                    archivosDisponibles.add(archivo);
                }

                if (!archivosDisponibles.isEmpty()) {
                    // INICIO ALEATORIO: Empezar desde un índice aleatorio
                    indiceActual = random.nextInt(archivosDisponibles.size());
                    inicializado = true;

                    System.out.println("Rotador " + prefijoArchivo + " inicializado:");
                    System.out.println("  - Total archivos: " + archivosDisponibles.size());
                    System.out.println("  - Índice inicial aleatorio: " + indiceActual);
                    System.out.println("  - Archivo inicial: " + archivosDisponibles.get(indiceActual).getName());
                    System.out.println("  - Secuencia ordenada:");
                    for (int i = 0; i < archivosDisponibles.size(); i++) {
                        System.out.println("    [" + i + "] " + archivosDisponibles.get(i).getName());
                    }
                }
            } else {
                System.out.println("No se encontraron archivos numerados en: " + rutaCarpeta);
                System.out.println("Archivos en la carpeta:");
                File[] todosArchivos = carpeta.listFiles();
                if (todosArchivos != null) {
                    for (File archivo : todosArchivos) {
                        boolean esAudio = esArchivoAudio(archivo.getName());
                        boolean tieneNumero = tieneNumeroAlInicio(archivo.getName());
                        System.out.println("  - " + archivo.getName() +
                                " (es audio: " + esAudio + ", tiene número: " + tieneNumero + ")");
                    }
                }
            }
        }

        /**
         * CORREGIDO: Verifica si el archivo tiene número al inicio (01, 02, etc.)
         */
        private boolean tieneNumeroAlInicio(String nombreArchivo) {
            // Buscar patrón de 2-3 dígitos al inicio del nombre
            java.util.regex.Pattern patron = java.util.regex.Pattern.compile("^(\\d{2,3})");
            java.util.regex.Matcher matcher = patron.matcher(nombreArchivo);
            return matcher.find();
        }

        /**
         * CORREGIDO: Extrae el número del archivo (debe existir)
         */
        private String extraerNumeroDeArchivo(String nombreArchivo) {
            // Buscar número de 2-3 dígitos al inicio
            java.util.regex.Pattern patron = java.util.regex.Pattern.compile("^(\\d{2,3})");
            java.util.regex.Matcher matcher = patron.matcher(nombreArchivo);

            if (matcher.find()) {
                return matcher.group(1);
            }

            // Si no encuentra al inicio, buscar en cualquier parte
            patron = java.util.regex.Pattern.compile("(\\d{2,3})");
            matcher = patron.matcher(nombreArchivo);

            if (matcher.find()) {
                return matcher.group(1);
            }

            return "999"; // Valor por defecto (se ordenará al final)
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
            System.out.println("  - Índice: " + indiceActual + "/" + (archivosDisponibles.size() - 1));
            System.out.println("  - Archivo: " + archivoActual.getName());
            System.out.println("  - Número: " + extraerNumeroDeArchivo(archivoActual.getName()));

            // SECUENCIAL: Avanzar al siguiente índice de forma secuencial (con reinicio)
            indiceActual = (indiceActual + 1) % archivosDisponibles.size();

            if (indiceActual == 0) {
                System.out.println("  - Reiniciando secuencia para " + prefijoArchivo);
            }

            System.out.println("  - Próximo índice: " + indiceActual + " (" +
                    (archivosDisponibles.isEmpty() ? "N/A" : archivosDisponibles.get(indiceActual).getName()) + ")");

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
                    insercion.setTipo(InsercionEspecial.TipoInsercion.IDENTIFICACION); // Usar el mismo tipo
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