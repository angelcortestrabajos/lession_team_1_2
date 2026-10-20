# Lession Team 1/2

Android application built with Kotlin and Jetpack Compose.

## Pruebas

Consulta los [casos de uso, casos de prueba y resultados](CASOS_DE_USO_Y_PRUEBAS.md).

## Sensor EMG BLE

La versión 1.1.0 puede conectarse al dispositivo `ESP32-C3-EMG` mediante el servicio
`4fafc201-1fb5-459e-8fcc-c5c9c331914b` y la característica
`beb5483e-36e1-4688-b7f5-ea07361b26a8`. Las notificaciones JSON válidas actualizan la
sesión con `value` en mV; `adcRaw` se conserva como lectura ADC independiente. El porcentaje,
si existe una referencia máxima válida guardada por el usuario, se calcula como
`(value_mV / referencia_mV) * 100` y se limita visualmente a 0–100 %. No es una medición
clínica ni una fuerza muscular calibrada.

La simulación es manual, empieza detenida y no se reactiva al abrir Inicio. Al conectarse
al sensor se detiene; detenerla no desconecta BLE. Reset borra solo las estadísticas y la
gráfica de la sesión EMG, manteniendo la referencia máxima y el historial permanente.
`READ_DATA` y los comandos LED existentes se escriben en la característica BLE indicada;
no se envían comandos de inicio o detención de adquisición que el firmware no reconoce.

La prueba física requiere el ESP32-C3 y no se considera aprobada por una compilación o
prueba unitaria.

## Firebase setup

To build and run the app locally, add the Firebase Android configuration file from your Firebase project at `app/google-services.json`. This file is ignored by Git and must not be committed.
