# Firmware ESP32-C3-EMG

El sketch `ESP32-C3-EMG/ESP32-C3-EMG.ino` implementa el protocolo BLE que consume la
aplicación Android: dispositivo `ESP32-C3-EMG`, servicio
`4fafc201-1fb5-459e-8fcc-c5c9c331914b` y característica
`beb5483e-36e1-4688-b7f5-ea07361b26a8`. Acepta `READ_DATA`, los comandos individuales
`LED_RED_*`, `LED_GREEN_*`, `LED_BLUE_*`, y `ALL_ON`/`ALL_OFF`. Con la lectura habilitada,
notifica una muestra JSON cada 100 ms.

## Pines declarados

| Función | Pin del ESP32-C3 SuperMini |
|---|---|
| Señal SIG del EMG | A0 / GPIO0 |
| Rojo | GPIO8 |
| Verde | GPIO9 |
| Azul | GPIO10 |

Cada canal del LED externo debe tener su propia resistencia limitadora. El sketch está
configurado para cátodo común; cambiar `kRgbCommonAnode` a `true` si el LED es de ánodo
común. GPIO8 y GPIO9 pueden intervenir en el arranque del ESP32-C3; si la placa deja de
iniciar correctamente con el LED conectado, usar GPIO alternativos y actualizar el mapa.

## Precaución eléctrica del SIG

El sensor informado usa alimentación simétrica (+4 V, 0 V y −4 V), pero no se ha medido el
rango de su salida SIG. El ADC del ESP32-C3 admite únicamente tensiones no negativas dentro
de su rango de entrada (máximo de diseño 3,3 V); el programa no puede protegerlo de una
tensión fuera de ese rango. Por eso `kSigRangeVerified` queda en `false` y las lecturas EMG
no se publican. No cambiarlo a `true` ni conectar SIG directamente hasta verificar,
respecto de 0 V, que la salida se mantiene entre 0 y 3,3 V en todas las condiciones. Si no,
hace falta una interfaz de acondicionamiento adecuada al sensor y al ADC.

Para medir SIG respecto de la referencia del ADC se necesita una referencia de señal
compatible entre el 0 V del sensor y GND del ESP32, o una interfaz aislada apropiada. No
conectar los rieles +4 V ni −4 V del sensor a la placa. Dos celdas 18650 cargadas pueden
alcanzar 4,2 V cada una; la tensión simétrica real puede ser aproximadamente ±4,2 V.

La aplicación espera `metric: "emgSignal"`, `value` en mV y `adcRaw` entre 0 y 4095. El
voltaje reportado es el medido en la entrada ADC; no es fuerza muscular, fatiga ni una
medición clínica. No
conectar el circuito a USB, cargadores ni equipos alimentados desde la red mientras haya
electrodos puestos. El uso con personas requiere validación eléctrica y supervisión
adecuadas.

## Carga

Abrir el `.ino` con Arduino IDE, instalar el paquete de placas ESP32 de Espressif y
seleccionar una placa ESP32-C3 compatible (por ejemplo, `ESP32C3 Dev Module`). El sketch
usa la biblioteca BLE incluida en ese paquete. La opción `kSigRangeVerified` solo debe
habilitarse después de verificar eléctricamente la salida SIG.
