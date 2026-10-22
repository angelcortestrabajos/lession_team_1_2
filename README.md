# Lession Team 1/2

Android application built with Kotlin and Jetpack Compose.

## Pruebas

Consulta los [casos de uso, casos de prueba y resultados](CASOS_DE_USO_Y_PRUEBAS.md).
La guía de uso está en [docs/manual-usuario.md](docs/manual-usuario.md).

## Sensor EMG BLE

La versión actual del proyecto (`3.1.0`) está preparada para conectarse al dispositivo
`ESP32-C3-EMG` mediante el servicio `4fafc201-1fb5-459e-8fcc-c5c9c331914b` y la
característica `beb5483e-36e1-4688-b7f5-ea07361b26a8`. Las notificaciones JSON válidas
actualizan la sesión con `value` en mV; `adcRaw` se conserva como lectura ADC independiente.
La calibración guiada promedia muestras en reposo y durante el esfuerzo máximo. El esfuerzo
relativo se calcula como `(value_mV - reposo_mV) / (máximo_mV - reposo_mV) * 100`, limitado
visualmente a 0–100 %. Al alcanzar el máximo calibrado, se emite una alerta de umbral, no un
diagnóstico ni una predicción clínica. La señal tampoco representa una fuerza muscular
calibrada.

La simulación es manual, empieza detenida y no se reactiva al abrir Inicio. Al conectarse
al sensor se detiene; detenerla no desconecta BLE. Reset borra solo las estadísticas y la
gráfica de la sesión EMG, manteniendo la calibración y el historial permanente.
`READ_DATA`, `LED_RED_ON/OFF`, `LED_GREEN_ON/OFF`, `LED_BLUE_ON/OFF`, `ALL_OFF` y
`ALL_ON` se escriben en la característica BLE indicada; no se envían comandos de inicio o
detención de adquisición que el firmware no reconoce.

Este repositorio no incluye el firmware de la ESP32. La conexión física, recepción de
muestras y control del LED todavía deben validarse con el hardware; una compilación o prueba
unitaria no demuestra que funcionen.

## Fuentes de datos y límites actuales

La integración BLE implementada recibe muestras EMG JSON del ESP32-C3, frecuencia cardíaca
del perfil GATT estándar `0x180D/0x2A37` y batería `0x180F/0x2A19`. Inicio y estadísticas no
interpretan esos servicios como prueba de que haya datos: la conexión y la recepción reciente
se muestran por separado. Las estadísticas escuchan los cambios del historial persistido en
Firestore y etiquetan los registros como BLE o demostración.

El firmware/protocolo actual no entrega mediciones BLE procesadas para SpO₂, estabilidad,
presión arterial ni temperaturas; esas métricas aparecen como «Sin datos» y no se usan para
afirmar que esos sensores estén conectados. El valor EMG tampoco se convierte a fatiga ni a
riesgo clínico. La simulación es una fuente de demostración separada. No hay configuración
Wi-Fi ni restablecimiento general de preferencias implementados en Ajustes. La conexión y
reconexión física deben validarse con el ESP32-C3; una compilación no demuestra su operación.

## Firebase setup

To build and run the app locally, add the Firebase Android configuration file from your Firebase project at `app/google-services.json`. This file is ignored by Git and must not be committed.
