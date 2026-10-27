# Documentación Completa de Casos de Uso y Casos de Prueba - AppLession

Este documento detalla el conjunto completo de **Casos de Uso (CU)** y **Casos de Prueba (TP)** implementados y verificados en la aplicación **AppLession**.

---

## 1. Catálogo de Casos de Uso (CU)

| ID | Módulo / Funcionalidad | Actor | Precondición | Descripción del Flujo | Postcondición |
|---|---|---|---|---|---|
| **CU-01** | Autenticación (Login) | Usuario | Tener una cuenta registrada | El usuario ingresa su correo y contraseña; el sistema valida las credenciales contra Firebase Auth. | Acceso concedido al Dashboard principal (Home). |
| **CU-02** | Registro e Invitado | Usuario | Sin cuenta previa | El usuario se registra con correo o ingresa mediante el botón de Acceso Rápido (Invitado). | Se crea la sesión de usuario y se cargan las preferencias por defecto. |
| **CU-03** | Recuperar Contraseña | Usuario | Correo registrado | El usuario solicita recuperación ingresando su email. El sistema envía un correo de restablecimiento. | Mensaje de confirmación de envío de correo. |
| **CU-04** | Monitoreo Biométrico | Usuario | Sesión iniciada | El usuario visualiza en tiempo real las métricas biométricas (Frecuencia cardíaca, SpO2, temperatura, fatiga muscular, estabilidad y riesgo). | Las tarjetas y el medidor circular de riesgo se actualizan en tiempo real. |
| **CU-05** | Gestión de Sensores BLE | Usuario | Bluetooth activado | El usuario busca dispositivos BLE, conecta el **ESP32 Super Mini** y verifica el estado de los 6 sensores físicos. | Conexión GATT establecida y recepción de telemetría multi-sensor activa. |
| **CU-06** | Consulta de Estadísticas | Usuario | Sesiones históricas previas | El usuario consulta gráficos y promedios agrupados por Día, 7 Días y Mes, respaldados por Room y Firestore. | Visualización correcta de tendencias estadísticas. |
| **CU-07** | Gestión de Entrenamientos | Usuario | Sesión activa | El usuario inicia un entrenamiento seleccionando la actividad y nivel de esfuerzo, registrando la sesión. | Sesión guardada con etiquetado de fuente (Real vs Simulación). |
| **CU-08** | Configuración de Perfil | Usuario | Sesión iniciada | El usuario edita su edad, peso, altura, género y deporte preferido. | Perfil actualizado en SharedPreferences y Firestore. |
| **CU-09** | Notificaciones de Alerta | Sistema / Usuario | Umbrales superados | El sistema emite alertas visuales (banner crítico/preventivo) cuando la fatiga o el ritmo cardíaco superan los límites seguros. | Notificación visible en la parte superior del Dashboard. |
| **CU-10** | Cambio de Idioma y Tema | Usuario | Menú de Ajustes | El usuario alterna entre Español/Inglés y Tema Claro/Oscuro. | La interfaz se actualiza de inmediato mediante CompositionLocals. |
| **CU-11** | Simulación y Reseteo | Usuario | Pantalla de Inicio | El usuario inicia la simulación (actualización cada 2s) o la detiene (reseteando instantáneamente todos los indicadores a cero). | Dashboard en cero al detener; datos regenerándose al iniciar. |

---

## 2. Catálogo de Casos de Prueba (TP)

| ID Prueba | Tipo de Prueba | Componente Probado | Entrada / Condición de Prueba | Resultado Esperado | Estado |
|---|---|---|---|---|---|
| **TP-01** | Unitaria | Algoritmo de Riesgo | Fatiga = 50%, Estabilidad = 90%, HR = 120 | Riesgo calculado = 38.0% | **Aprobado** |
| **TP-02** | Unitaria | Parsing BLE (8 bits) | Paquete BLE estándar `[0x00, 0x4E]` | Ritmo cardíaco extraído = 78 BPM | **Aprobado** |
| **TP-03** | Unitaria | Parsing BLE (16 bits) | Paquete BLE estándar `[0x01, 0x8C, 0x00]` | Ritmo cardíaco extraído = 140 BPM | **Aprobado** |
| **TP-04** | Unitaria | Sesión EMG y mV | Muestras de 500 mV y 1200 mV con referencia máxima 1000 mV | Promedio = 850 mV, Rendimiento acotado al 100% | **Aprobado** |
| **TP-05** | Unitaria | Rechazo de Datos Invalidos | Muestra EMG con `NaN`, valores negativos o ADC > 4095 | La sesión ignora la muestra inválida | **Aprobado** |
| **TP-06** | Unitaria | Agregación de Estadísticas | Lista de entradas históricas con valores nulos o fuera de rango | Filtrado correcto de estadísticas válidas y promedio exacto | **Aprobado** |
| **TP-07** | Integración BLE | Conexión GATT | Timeout de 15s para sub-sensores | Transición automática a estado `DISCONNECTED` si no hay paquetes | **Aprobado** |
| **TP-08** | Integración UI | Detener Simulación | Pulsar botón "Detener simulación" | Reseteo inmediato de BPM, fatiga, estabilidad y riesgo a 0 | **Aprobado** |
| **TP-09** | Integración UI | Origen de Datos | Recepción de telemetría de ESP32 Super Mini | Visualización del badge "Sensores reales activos" en verde | **Aprobado** |
| **TP-10** | Integración UI | Marca de Agua de Lession | Verificación de icono de lanzador (`ic_launcher`) | Ícono personalizado de mancuerna/fitness corporativo | **Aprobado** |
| **TP-11** | Compilación | Gradle Build | Ejecución de `assembleDebug` | `BUILD SUCCESSFUL` sin errores de compilación | **Aprobado** |
| **TP-12** | Instalación USB | ADB Streamed Install | Despliegue de APK en dispositivo físico Android 12-15 | `Success` y ejecución limpia en MainActivity | **Aprobado** |
