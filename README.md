# AgroTech para Android

Primera maqueta nativa en **Kotlin**, basada en el boceto aprobado de Rancho El Encino. Usa controles de Android y mantiene los datos de demostración en memoria. No contiene servidores, bases de datos ni solicitudes de red.

## Abrir en Android Studio

1. En Android Studio, selecciona **Open** y abre la carpeta `AgroTechAndroid`, donde está este archivo. No abras solo `app`.
2. Espera a que termine la sincronización de Gradle. La primera vez requiere internet para descargar herramientas y dependencias.
3. Si se solicita el SDK, instala **Android SDK Platform 36** y **Build Tools 36.0.0** desde **Tools → SDK Manager**. Usa el JDK incluido con Android Studio; Gradle 9.1 admite Java 17 a 25.
4. Selecciona un emulador o un teléfono Android con depuración USB habilitada y pulsa **Run ▶**. Compatible con Android 8.0 (API 26) en adelante.

Si el SDK está en otra ubicación, Android Studio creará el archivo `local.properties`. Ese archivo depende de tu computadora y no se distribuye con el proyecto.

## Recorrido para la presentación

- **Inicio:** accesos grandes a Agregar, Consultar y Métricas.
- **Consultar:** inventario de hembras, sementales y crías.
- **Ficha:** datos básicos, categoría, edad, corral y último peso. La cría 101 enlaza con su madre 080 y su padre 019; estos enlazan de regreso con la cría.
- **Agregar:** registra un animal de ejemplo con arete único, sexo, categoría, raza, fecha de nacimiento y SINIIGA opcional. Prueba con el arete 102.
- **Pesos:** desde la ficha, abre el historial y agrega un peso en una fecha entre el nacimiento y hoy que todavía no tenga pesaje. Acepta punto o coma decimal y ordena el historial por fecha.
- **Métricas:** conteos calculados con los animales presentes en la demostración. Cambian al agregar animales.

Los datos ficticios parten de la fecha actual del dispositivo. La app conserva los cambios al girar la pantalla o navegar. Si Android termina el proceso de la app, la demostración vuelve a sus datos iniciales. No se promete persistencia al cerrar la app. Los borradores de formularios se conservan al navegar y al recrear la pantalla.

## Código

```text
app/src/main/java/mx/edu/agrotech/
  MainActivity.kt           Pantallas, navegación y formularios
  DemoApplication.kt        Repositorio en memoria por sesión
  data/Animal.kt            Modelo, categorías, sexo y pesajes
  data/DemoRepository.kt    Altas, consultas y validaciones
  ui/LineIcon.kt            Iconos vectoriales nativos
app/src/main/res/           Tema e icono de la app
app/src/test/               Pruebas de reglas del repositorio
```

Las categorías y el sexo se almacenan por separado. El repositorio está separado de la interfaz para poder sustituir los datos de demostración cuando se diseñen el servidor y el almacenamiento local.

## Alcance de esta entrega

Se implementaron navegación, altas de prueba, ficha, parentescos, pesajes y conteos. Se valida arete duplicado, nacimiento futuro, coherencia de sexo y categoría, peso válido, fecha de pesaje y duplicados por fecha. El límite de 500 kg es una validación de demostración, pendiente de validar con los asesores.

Quedan para etapas posteriores: acceso por usuarios y permisos, nacimiento formal con evidencia y bloqueo, fotos, sanidad, dietas, destete, exportación de Excel, notificaciones, bases de datos y sincronización. No se muestran porcentajes de preñez ni utilidades financieras inventadas.

## Compilación y pruebas

En macOS/Linux:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

En Windows usa `gradlew.bat` con los mismos argumentos. El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Herramientas fijadas: Android Gradle Plugin 9.0.1, Gradle 9.1.0 y SDK 36; objetivo Android API 35. Compatibilidad documentada por [Android Developers](https://developer.android.com/build/releases/agp-9-0-0-release-notes) y [Gradle](https://docs.gradle.org/9.1.0/userguide/compatibility.html).

Consulta `VERIFICACION.md` para los resultados de compilación y pruebas, y `PREGUNTAS.md` para las dudas de requisitos que conviene llevar a la próxima reunión.
