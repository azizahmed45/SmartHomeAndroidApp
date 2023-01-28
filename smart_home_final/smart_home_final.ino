#include <ESP8266WiFi.h>
#include <PubSubClient.h>

#include <DHT.h>
#define DHTPIN 5
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

int counter = 0;

const char* ssid = "SEU-3RD";
const char* password = "20192019";
const char* mqtt_server = "broker.mqttdashboard.com";

int led1 = 2;
int led2 = 4;

int buzzer = 14;

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);
  //led output
  pinMode(led1, OUTPUT);
  pinMode(led2, OUTPUT);
  pinMode(buzzer, OUTPUT);

  digitalWrite(led1, HIGH);
  digitalWrite(led2, HIGH);
  digitalWrite(buzzer, LOW);

  counter = 0;

  dht.begin();

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");

  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  //dht sensor data
  float h = dht.readHumidity();
  float t = dht.readTemperature();

  counter++;

  int smokeData = analogRead(A0);

  if (counter > 30) {
    client.publish("seu2019-data", String("humidity:" + String(h)).c_str());
    client.publish("seu2019-data", String("temperature:" + String(t)).c_str());

    client.publish("seu2019-data", String("smoke:" + String(smokeData)).c_str());
    counter = 0;
  }

  if (smokeData > 600) {
    digitalWrite(buzzer, HIGH);
  } else {
    digitalWrite(buzzer, LOW);
  }

  delay(100);
}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "seu2019-";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (client.connect(clientId.c_str())) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      client.publish("outTopic", "hello world");
      // ... and resubscribe
      client.subscribe("seu2019-switch");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void callback(char* topic, byte* payload, unsigned int length) {
  char message[length + 1];
  for (int i = 0; i < length; i++) {
    message[i] = (char)payload[i];
  }
  message[length] = '\0';

  if (strcmp(message, "switch1:on") == 0) {
    // do something when the message is "switch1:on"
    digitalWrite(led1, LOW);
    Serial.println("Switch 1 is On");
  } else if (strcmp(message, "switch1:off") == 0) {
    // do something when the message is "switch1:off"
    digitalWrite(led1, HIGH);
    Serial.println("Switch 1 is off");
  } else if (strcmp(message, "switch2:on") == 0) {
    // do something when the message is "switch2:on"
    digitalWrite(led2, LOW);
    Serial.println("Switch 2 is On");
  } else if (strcmp(message, "switch2:off") == 0) {
    // do something when the message is "switch2:off"
    digitalWrite(led2, HIGH);
    Serial.println("Switch 2 is off");
  }
}
