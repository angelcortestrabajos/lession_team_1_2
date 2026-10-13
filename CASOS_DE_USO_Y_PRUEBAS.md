# Casos de uso y pruebas

**Aplicación:** Lession Team 1/2

**Versión probada:** 1.0 (debug)

**Fecha de ejecución:** 13 de octubre de 2026

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

| ID | Caso | Pasos resumidos | Resultado esperado | Estado |
|---|---|---|---|---|
| CP-07 | Acceso válido | Ingresar una cuenta de prueba válida y una contraseña correcta. | Se inicia sesión y aparece Inicio. | PENDIENTE: requiere cuenta de prueba. |
| CP-08 | Acceso inválido | Ingresar una contraseña incorrecta para una cuenta de prueba. | No se inicia sesión y se informa el error. | PENDIENTE: requiere cuenta de prueba. |
| CP-09 | Registro | Registrar una cuenta de prueba nueva con nombre, correo y contraseña válidos. | Se informa el envío del correo de verificación y no se crea una sesión antes de la verificación. | PENDIENTE: requiere cuenta/correo de prueba. |
| CP-10 | Acceso de invitado | Usar “Acceso Rápido (Invitado)”. | Se abre Inicio y se pueden consultar las pantallas compatibles con el invitado. | PENDIENTE. |
| CP-11 | Recuperar contraseña | Abrir “Olvidaste tu contraseña”, enviar un correo de prueba válido y revisar el estado. | La aplicación confirma el envío o muestra el error del servicio. | PENDIENTE: requiere correo de prueba. |
| CP-12 | Completar datos corporales | Abrir el formulario, enviar datos válidos y luego probar campos vacíos o fuera de rango. | Los datos válidos se guardan y los inválidos reciben una indicación comprensible. | PENDIENTE: requiere una sesión válida. |
| CP-13 | Editar perfil | Cambiar nombre, edad, peso, altura, género y deporte; guardar y reabrir. | Los datos actualizados se conservan y se ven reflejados en el perfil. | PENDIENTE: requiere una sesión válida. |
| CP-14 | Señales y estadísticas | Consultar Inicio, Estado e informes; contrastar valores e historial. | Las pantallas cargan sin cierres y muestran datos coherentes. | PENDIENTE: requiere datos de prueba. |
| CP-15 | Bluetooth con y sin permisos | Denegar y conceder los permisos; repetir con un sensor compatible disponible. | La app gestiona ambos permisos y comunica claramente conexión o falta de dispositivo. | PENDIENTE: requiere dispositivo BLE de prueba. |
| CP-16 | Crear y consultar objetivos | Crear un objetivo, volver a la lista y abrir un entrenamiento relacionado. | El objetivo aparece y sus datos permanecen disponibles al volver a la pantalla. | PENDIENTE: requiere servicio/datos de prueba. |
| CP-17 | Historial de entrenamientos | Consultar el historial, abrir un registro y regresar. | Los registros disponibles se muestran y la navegación de detalle funciona. | PENDIENTE: requiere registros de prueba. |
| CP-18 | Notificaciones | Generar o recibir una alerta y abrir la lista de notificaciones. | La alerta se muestra y se puede marcar como leída cuando corresponda. | PENDIENTE: requiere datos/eventos de prueba. |
| CP-19 | Tema e idioma | Cambiar cada preferencia desde Configuración y reiniciar la aplicación. | La interfaz cambia y la preferencia se conserva según lo esperado. | PENDIENTE. |
| CP-20 | Cerrar sesión | Cerrar sesión desde Perfil y reabrir la aplicación. | Se cierra la sesión y se vuelve a la pantalla de acceso. | PENDIENTE: requiere una sesión válida. |
| CP-21 | Eliminar cuenta | Iniciar el flujo, cancelar la confirmación y luego probar con una cuenta desechable. | Cancelar no elimina la cuenta; confirmar elimina solo la cuenta de prueba. | PENDIENTE: requiere cuenta desechable. |
| CP-22 | Revisión de permisos | Revisar los permisos Bluetooth, ubicación y notificaciones en Android 12 y una versión reciente. | La aplicación solicita los permisos necesarios en el momento correcto y ofrece alternativas si se deniegan. | PENDIENTE. |

## Resumen y limitaciones

- La compilación debug, la prueba unitaria de ejemplo y las dos pruebas instrumentadas existentes pasaron.
- Se comprobó manualmente la instalación e inicio de la aplicación en un Moto G22 con Android 12.
- Android Lint reportó 34 advertencias y 1 aviso; el detalle está en `app/build/reports/lint-results-debug.html` al generar ese informe.
- La cobertura automática existente es de humo, no una validación funcional completa.
- Los flujos con servicios externos deben verificarse con cuentas y datos de prueba autorizados. No se deben incluir contraseñas, tokens ni archivos privados de Firebase en los reportes o en GitHub.
