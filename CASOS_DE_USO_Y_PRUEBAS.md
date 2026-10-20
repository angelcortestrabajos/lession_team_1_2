# Casos de uso y pruebas

**Aplicación:** Lession Team 1/2

**Versión probada:** 1.1.0 (debug)

Las pruebas de la versión 1.1.0 se ejecutaron antes de publicar el commit en GitHub.

Este documento separa las comprobaciones realmente ejecutadas de las pruebas funcionales pendientes. Un resultado satisfactorio al compilar o abrir la aplicación no significa que se hayan probado todas sus funciones.

## Casos de uso

| ID | Caso de uso | Resultado esperado |
|---|---|---|
| CU-01 | Iniciar sesión con correo y contraseña | Se valida la cuenta y se abre la pantalla principal. Si los datos no son válidos, se muestra un error. |
| CU-02 | Registrarse o acceder como invitado | El registro informa que se envió el correo de verificación; el acceso de invitado abre la aplicación sin registrar una cuenta normal. |
| CU-03 | Recuperar contraseña | Se solicita un enlace para restablecer la contraseña y se informa si el envío fue exitoso. |
| CU-04 | Consultar el inicio y las señales biométricas | Se muestra el resumen biométrico del usuario y se actualizan sus datos o alertas. |
| CU-05 | Conectar un dispositivo Bluetooth | Se solicita el permiso necesario, se muestran los dispositivos disponibles y se informa el estado de conexión. |
| CU-06 | Consultar estadísticas e historial | Se pueden ver las estadísticas y los registros disponibles para la cuenta. |
| CU-07 | Gestionar entrenamientos y objetivos | Se pueden consultar entrenamientos, acceder a su detalle y crear o revisar objetivos. |
| CU-08 | Consultar y configurar el perfil | Se muestran los datos y métricas del usuario; se pueden editar el perfil y los parámetros corporales. |
| CU-09 | Consultar las notificaciones | Las alertas y notificaciones disponibles se muestran en la pantalla correspondiente. |
| CU-10 | Cambiar idioma y tema | Las preferencias se modifican desde Configuración y se conservan según la implementación de la aplicación. |
| CU-11 | Cerrar o eliminar la cuenta | Cerrar sesión vuelve al acceso; eliminar la cuenta pide confirmación antes de continuar. |

## Pruebas ejecutadas

| ID | Tipo | Prueba | Resultado | Observaciones |
|---|---|---|---|---|
| CP-01 | Unitaria | Ejecutar `ExampleUnitTest.addition_isCorrect` | APROBADA | Es una prueba de ejemplo (`2 + 2`); no verifica una regla funcional del producto. |
| CP-02 | Instrumentada | Comprobar que el contexto de Android usa el paquete esperado | APROBADA | Ejecutada en el Moto G22 con Android 12. |
| CP-03 | Instrumentada | Abrir `MainActivity` y comprobar que no finaliza inmediatamente | APROBADA | La pantalla principal de la aplicación se pudo iniciar en el dispositivo conectado. |
| CP-04 | Manual / humo | Instalar el APK debug e iniciar la actividad principal | APROBADA | Android informó `Status: ok`; la actividad de la aplicación permaneció en primer plano, sin excepción fatal en la comprobación. |
| CP-05 | Compilación | Ejecutar `assembleDebug` | APROBADA | Se generó el APK de depuración. |
| CP-06 | Análisis estático | Ejecutar `lintDebug` | APROBADA CON OBSERVACIONES | El análisis terminó con 34 advertencias y 1 aviso. No equivale a 35 fallos funcionales. |

### Comandos para repetir las pruebas automáticas

En Windows, desde la raíz del proyecto:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon
```

`connectedDebugAndroidTest` requiere un emulador iniciado o un dispositivo Android conectado con depuración USB habilitada.

## Casos de prueba funcionales pendientes

Los siguientes escenarios están identificados, pero **todavía no se han probado de extremo a extremo**. No marcarlos como aprobados hasta ejecutarlos y registrar evidencia.

| ID    | Caso | Pasos resumidos | Resultado esperado | Estado |
|-------|---|---|---|---|
| CP-07 | Acceso válido | Ingresar una cuenta de prueba válida y una contraseña correcta. | Se inicia sesión y aparece Inicio. | PENDIENTE: requiere cuenta de prueba. |
| CP-08 | Acceso inválido | Ingresar una contraseña incorrecta para una cuenta de prueba. | No se inicia sesión y se informa el error. | PENDIENTE: requiere cuenta de prueba. |
| CP-09 | Registro | Registrar una cuenta de prueba nueva con nombre, correo y contraseña válidos. | Se informa el envío del correo de verificación y no se crea una sesión antes de la verificación. | PENDIENTE: requiere cuenta/correo de prueba. |
| CP-11 | Recuperar contraseña | Abrir “Olvidaste tu contraseña”, enviar un correo de prueba válido y revisar el estado. | La aplicación confirma el envío o muestra el error del servicio. | PENDIENTE: requiere correo de prueba. |
| CP-12 | Completar datos corporales | Abrir el formulario, enviar datos válidos y luego probar campos vacíos o fuera de rango. | Los datos válidos se guardan y los inválidos reciben una indicación comprensible. | PENDIENTE: requiere una sesión válida. |
| CP-13 | Editar perfil | Cambiar nombre, edad, peso, altura, género y deporte; guardar y reabrir. | Los datos actualizados se conservan y se ven reflejados en el perfil. | PENDIENTE: requiere una sesión válida. |
| CP-14 | Señales y estadísticas | Consultar Inicio, Estado e informes; contrastar valores e historial. | Las pantallas cargan sin cierres y muestran datos coherentes. | PENDIENTE: requiere datos de prueba. |
| CP-15 | Bluetooth con y sin permisos | Denegar y conceder los permisos; repetir con un sensor compatible disponible. | La app gestiona ambos permisos y comunica claramente conexión o falta de dispositivo. | PENDIENTE: requiere dispositivo BLE de prueba. |
| CP-19 | Revisión de permisos | Revisar los permisos Bluetooth, ubicación y notificaciones en Android 12 y una versión reciente. | La aplicación solicita los permisos necesarios en el momento correcto y ofrece alternativas si se deniegan. | PENDIENTE. |

## Resumen y limitaciones

- La compilación debug, las 10 pruebas unitarias (1 de ejemplo y 9 de sesión EMG) y las 2 pruebas instrumentadas pasaron.
- Se comprobó manualmente la instalación e inicio de la aplicación en un Moto G22 con Android 12.
- Android Lint reportó 36 advertencias y 1 sugerencia; el detalle está en `app/build/reports/lint-results-debug.html`.
- La cobertura automática incluye las reglas del modelo EMG y pruebas instrumentadas de humo, no una validación funcional completa.
- Los flujos con servicios externos deben verificarse con cuentas y datos de prueba autorizados. No se deben incluir contraseñas, tokens ni archivos privados de Firebase en los reportes o en GitHub.

## Versión 1.1.0: sensor EMG y modos de medición

La versión siguiente incorpora el flujo BLE del `ESP32-C3-EMG` (servicio
`4fafc201-1fb5-459e-8fcc-c5c9c331914b`, característica
`beb5483e-36e1-4688-b7f5-ea07361b26a8`), recepción de muestras JSON en mV, comandos del
firmware, simulación explícita, Reset de sesión EMG y referencia Máximo persistida.

Las pruebas automatizadas de esta versión validan acumulación/promedio/máximo EMG,
rechazo de muestra inválida o repetida, porcentaje limitado y Reset que conserva la
referencia. Las pruebas de descubrimiento, conexión, CCCD, notificaciones, comandos,
desconexión y reconexión siguen requiriendo un ESP32-C3 físico y un Android compatible;
no se afirma verificación física hasta que se ejecute ese ensayo.

| ID | Tipo | Prueba | Resultado | Observaciones |
|---|---|---|---|---|
| CP-20 | Unitaria | Acumular mV reales, promedio, máximo y porcentaje limitado | APROBADA | `:app:testDebugUnitTest` |
| CP-21 | Unitaria | Ignorar timestamp duplicado, estado inválido, métrica distinta y ADC fuera de rango | APROBADA | `:app:testDebugUnitTest` |
| CP-22 | Unitaria | Evitar división por referencia cero y Reset de sesión conservando referencia | APROBADA | `:app:testDebugUnitTest` |
| CP-23 | Compilación | Generar APK debug 1.1.0 | APROBADA | `:app:assembleDebug` |
| CP-24 | Física BLE | Conectar, recibir notificaciones JSON y enviar comandos al ESP32-C3 | PENDIENTE | No había un ESP32-C3 disponible; no se afirma verificación física. |
| CP-25 | Instrumentada | Ejecutar las pruebas Android en un dispositivo conectado | APROBADA | `:app:connectedDebugAndroidTest`; dispositivo SM-A065M con Android 16, 2 pruebas. |

## Pruebas funcionales aún pendientes en 1.1.0

Las pruebas unitarias cubren las reglas del modelo de sesión EMG, pero no sustituyen
las pruebas de interacción de la aplicación ni el ensayo con el sensor físico.

| Caso | Estado | Motivo |
|---|---|---|
| Detener la simulación y confirmar que cesan las actualizaciones e inserciones posteriores en historial | PENDIENTE | No se ejecutó una prueba funcional automatizada que observe el ViewModel y el historial después de detenerlo. |
| Probar Reset con sensor conectado, desconectado y simulación detenida | PENDIENTE | Las pruebas unitarias verifican el reinicio del modelo, pero falta validar las transiciones de UI y BLE de extremo a extremo. |
| Guardar Máximo y verificar el porcentaje tras reiniciar la aplicación | PENDIENTE | La fórmula y los casos límite se prueban unitariamente; falta una prueba de persistencia y flujo de interfaz. |
| Probar permisos, descubrimiento, conexión, notificaciones, comandos, desconexión y reconexión BLE | PENDIENTE | Requiere ESP32-C3-EMG físico; CP-24 sigue pendiente. |
