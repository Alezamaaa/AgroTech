# Verificación de la maqueta

Verificación realizada el 15 de noviembre de 2026.

- Compilación `:app:assembleDebug`: correcta; APK debug generado y firmado para pruebas.
- Pruebas `:app:testDebugUnitTest`: 7 aprobadas, 0 fallos.
- Análisis `:app:lintDebug`: aprobado, sin errores. Conserva advertencias de compatibilidad, herramientas, traducción e iconos personalizados; no se deshabilitó la comprobación de errores.
- Wrapper de Gradle: hash cotejado con el publicado por Gradle; la distribución fija su SHA-256.

Las pruebas verifican las referencias familiares, altas y conteos, aretes duplicados, fechas, coherencia de sexo/categoría, orden e integridad de pesajes y protección de las colecciones del repositorio.

No se ejecutó la app en un teléfono o emulador. Está pendiente revisar visualmente tamaños de pantalla, teclado, barras del sistema, selector de fechas, rotación y navegación con el gesto Atrás en un dispositivo. La compilación y las pruebas de datos no sustituyen esa revisión.

El APK es una compilación debug para demostración académica; no una versión de publicación. Las fechas de los datos ficticios se adaptan al día del dispositivo.
