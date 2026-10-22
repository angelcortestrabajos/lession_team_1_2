#include <Arduino.h>
#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>

#include <atomic>
#include <cstdio>
#include <string>

namespace {

constexpr char kDeviceName[] = "ESP32-C3-EMG";
constexpr char kServiceUuid[] = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
constexpr char kCharacteristicUuid[] = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

constexpr uint8_t kEmgPin = 0;  // A0 / GPIO0 en ESP32-C3 SuperMini.
constexpr uint8_t kRedPin = 8;
constexpr uint8_t kGreenPin = 9;
constexpr uint8_t kBluePin = 10;
constexpr bool kRgbCommonAnode = false;

// Activar solo después de verificar que SIG siempre queda entre 0 y 3,3 V
// respecto de GND. Esta opción no protege eléctricamente el pin ADC.
constexpr bool kSigRangeVerified = false;

constexpr uint32_t kSampleIntervalMs = 100;
constexpr uint8_t kAdcSamplesPerReading = 8;

BLECharacteristic* emgCharacteristic = nullptr;
BLEAdvertising* advertising = nullptr;
volatile bool deviceConnected = false;
std::atomic<bool> readRequested{false};
bool redOn = false;
bool greenOn = false;
bool blueOn = false;
uint32_t lastSampleMs = 0;

void writeLed(uint8_t pin, bool on) {
  const uint8_t activeLevel = kRgbCommonAnode ? LOW : HIGH;
  digitalWrite(pin, on ? activeLevel : !activeLevel);
}

void applyLedState() {
  writeLed(kRedPin, redOn);
  writeLed(kGreenPin, greenOn);
  writeLed(kBluePin, blueOn);
}

void publishEmgSample() {
  if (!deviceConnected || !kSigRangeVerified || emgCharacteristic == nullptr) {
    return;
  }

  uint32_t rawTotal = 0;
  uint32_t millivoltsTotal = 0;
  for (uint8_t i = 0; i < kAdcSamplesPerReading; ++i) {
    rawTotal += static_cast<uint32_t>(analogRead(kEmgPin));
    millivoltsTotal += analogReadMilliVolts(kEmgPin);
  }

  const uint32_t adcRaw = rawTotal / kAdcSamplesPerReading;
  const uint32_t valueMv = millivoltsTotal / kAdcSamplesPerReading;
  char payload[192];
  const int length = std::snprintf(
      payload,
      sizeof(payload),
      "{\"type\":\"emg\",\"sensor\":\"emg_module\","
      "\"metric\":\"emgSignal\",\"value\":%lu,\"unit\":\"mV\","
      "\"adcRaw\":%lu,\"status\":\"valid\",\"timestamp\":%lu}",
      static_cast<unsigned long>(valueMv),
      static_cast<unsigned long>(adcRaw),
      static_cast<unsigned long>(millis()));

  if (length > 0 && static_cast<size_t>(length) < sizeof(payload)) {
    emgCharacteristic->setValue(
        reinterpret_cast<uint8_t*>(payload),
        static_cast<size_t>(length));
    emgCharacteristic->notify();
  }
}

class ServerCallbacks final : public BLEServerCallbacks {
  void onConnect(BLEServer*) override {
    deviceConnected = true;
  }

  void onDisconnect(BLEServer*) override {
    deviceConnected = false;
    if (advertising != nullptr) {
      advertising->start();
    }
  }
};

class CharacteristicCallbacks final : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* characteristic) override {
    std::string value = characteristic->getValue();
    String command(value.c_str());
    command.trim();

    if (command == "LED_RED_ON") {
      redOn = true;
    } else if (command == "LED_RED_OFF") {
      redOn = false;
    } else if (command == "LED_GREEN_ON") {
      greenOn = true;
    } else if (command == "LED_GREEN_OFF") {
      greenOn = false;
    } else if (command == "LED_BLUE_ON") {
      blueOn = true;
    } else if (command == "LED_BLUE_OFF") {
      blueOn = false;
    } else if (command == "ALL_OFF") {
      redOn = false;
      greenOn = false;
      blueOn = false;
    } else if (command == "ALL_ON") {
      redOn = true;
      greenOn = true;
      blueOn = true;
    } else if (command == "READ_DATA") {
      readRequested.store(true);
      return;
    } else {
      return;
    }

    applyLedState();
  }
};

}  // namespace

void setup() {
  Serial.begin(115200);

  pinMode(kRedPin, OUTPUT);
  pinMode(kGreenPin, OUTPUT);
  pinMode(kBluePin, OUTPUT);
  applyLedState();

  if (kSigRangeVerified) {
    analogReadResolution(12);
    analogSetPinAttenuation(kEmgPin, ADC_11db);
  } else {
    Serial.println(
        "EMG ADC desactivado: verifica primero que SIG este entre 0 y 3,3 V.");
  }

  BLEDevice::init(kDeviceName);
  BLEDevice::setMTU(247);

  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService* service = server->createService(kServiceUuid);
  emgCharacteristic = service->createCharacteristic(
      kCharacteristicUuid,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY);
  emgCharacteristic->addDescriptor(new BLE2902());
  emgCharacteristic->setCallbacks(new CharacteristicCallbacks());
  service->start();

  advertising = server->getAdvertising();
  advertising->addServiceUUID(kServiceUuid);
  advertising->setScanResponse(true);
  advertising->start();
}

void loop() {
  const uint32_t now = millis();
  if (readRequested.exchange(false)) {
    publishEmgSample();
  }
  if (kSigRangeVerified && now - lastSampleMs >= kSampleIntervalMs) {
    lastSampleMs = now;
    publishEmgSample();
  }
  delay(1);
}
