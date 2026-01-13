
# 🏦 Event-Driven Banking System (EDA)

Este proyecto es una prueba de concepto de un sistema bancario distribuido utilizando una **Arquitectura Orientada a Eventos (EDA)**. El objetivo principal es demostrar cómo evitar el antipatrón del "Monolito Distribuido" eliminando la comunicación síncrona (REST/HTTP) entre microservicios, favoreciendo el desacoplamiento total y la alta disponibilidad mediante el uso de **Apache Kafka** y **Proyecciones de Datos**.

## 🚀 Arquitectura y Filosofía

En una arquitectura de microservicios tradicional, un servicio suele llamar a otro vía HTTP (ej: Transacciones llama a Cuentas para ver el saldo). Esto crea acoplamiento temporal y puntos únicos de fallo.

**En este sistema:**
1.  **Cero Comunicación Síncrona entre Servicios:** Los microservicios no conocen las IPs ni los endpoints de los otros.
2.  **Coreografía sobre Orquestación:** Los servicios reaccionan a hechos (eventos) ocurridos en el pasado.
3.  **Proyecciones (CQRS Lite):** Cada microservicio almacena localmente los datos que necesita de otros dominios para operar.
    *   *Ejemplo:* `ms-transactions` no pregunta el saldo a `ms-accounts`. `ms-transactions` mantiene una copia de lectura del saldo actualizada en tiempo real escuchando eventos de Kafka.

### 🏛️ Bounded Contexts

El sistema está dividido en 3 dominios estrictos:

1.  **`ms-customers` (Identity Context):**
    *   **Responsabilidad:** "Source of Truth" de la información de clientes.
    *   **Evento Principal:** `CustomerCreatedEvent`.
2.  **`ms-accounts` (Product & Ledger Context):**
    *   **Responsabilidad:** "Source of Truth" de las cuentas y saldos oficiales.
    *   **Proyección:** Mantiene una copia local de Clientes para validar titularidad sin llamadas externas.
    *   **Evento Principal:** `AccountCreatedEvent`.
3.  **`ms-transactions` (Movement Context):**
    *   **Responsabilidad:** Gestión de transferencias y movimientos.
    *   **Proyección:** Mantiene una copia local de Cuentas y Saldos (AccountSummary) para validaciones de fondos ultrarrápidas y resilientes.
    *   **Evento Principal:** `TransactionCreatedEvent`.

## 🛠️ Stack Tecnológico

*   **Lenguaje:** Java 21+
*   **Framework:** Spring Boot 3.x (Web, Data JPA, Kafka)
*   **Messaging:** Apache Kafka (Confluent Platform) & Zookeeper.
*   **Database:** PostgreSQL (Instancia única dockerizada con bases de datos lógicas separadas).
*   **Infraestructura:** Docker & Docker Compose.
*   **Herramientas:** Lombok, Jackson.

## 🏆 Beneficios de esta Implementación

1.  **Desacoplamiento Temporal:** Si `ms-accounts` se cae, `ms-transactions` **sigue funcionando**. Puede procesar transferencias basándose en su última proyección conocida.
2.  **Latencia Mínima:** Las validaciones de negocio (¿Existe el usuario? ¿Tiene saldo?) se resuelven con consultas SQL locales (<5ms) en lugar de cadenas de llamadas HTTP (>100ms).
3.  **Alta Cohesión:** Cada servicio es autónomo. La lógica de negocio no se "filtra" entre servicios.
4.  **Escalabilidad Independiente:** Si el volumen de transacciones aumenta, podemos escalar horizontalmente `ms-transactions` sin afectar la carga de `ms-customers`.

---

## ⚙️ Guía de Instalación y Ejecución

### Prerrequisitos
*   Docker y Docker Compose instalados.
*   JDK 21 o superior.
*   Maven.

### 1. Levantar Infraestructura
Ejecuta el archivo de composición para levantar Kafka, Zookeeper y Postgres.

```bash
docker-compose up -d
```

*Verificar:*
*   **Kafka UI:** http://localhost:8080
*   **Postgres:** Puerto 5432

### 2. Compilar y Ejecutar Servicios
En terminales separadas, levanta cada microservicio (o usa tu IDE):

```bash
# Terminal 1: Clientes
cd ms-customers
mvn spring-boot:run

# Terminal 2: Cuentas
cd ms-accounts
mvn spring-boot:run

# Terminal 3: Transacciones
cd ms-transactions
mvn spring-boot:run
```

---

## 🧪 Caso de Uso (Paso a Paso)

Vamos a simular un flujo completo: Crear cliente -> Crear cuenta -> Transferir dinero.

### Paso 1: Crear Cliente (Fuente de la Verdad)
`ms-customers` guarda y emite evento.

```bash
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" \
  -d '{ "fullName": "Juan Perez", "email": "juan@test.com" }'
```
> **Efecto:** `ms-accounts` recibe el evento y crea una proyección interna del cliente.

### Paso 2: Crear Cuenta (Validación vía Proyección)
`ms-accounts` verifica localmente que el cliente existe y crea la cuenta.

```bash
curl -X POST http://localhost:8082/api/accounts \
  -H "Content-Type: application/json" \
  -d '{ "customerId": 1, "initialBalance": 1000.00 }'
```
> **Efecto:** `ms-transactions` recibe el evento y crea una proyección interna de la cuenta con saldo $1000.

*(Opcional) Crea una segunda cuenta para recibir dinero:*
```bash
# Crear cliente 2
curl -X POST http://localhost:8081/api/customers -H "Content-Type: application/json" -d '{ "fullName": "Maria Lopez", "email": "maria@test.com" }'
# Crear cuenta cliente 2 con saldo 0
curl -X POST http://localhost:8082/api/accounts -H "Content-Type: application/json" -d '{ "customerId": 2, "initialBalance": 0.00 }'
```

### Paso 3: Realizar Transferencia (Sin llamadas HTTP internas)
`ms-transactions` valida saldo contra su DB local y procesa.

```bash
curl -X POST http://localhost:8083/api/transactions \
  -H "Content-Type: application/json" \
  -d '{ "fromAccountId": 1, "toAccountId": 2, "amount": 200.00 }'
```

### Paso 4: Verificación Final
Si revisas los logs de `ms-accounts`, verás que recibió el evento de transacción y actualizó el saldo "Ledger" oficial. La consistencia eventual se ha completado.

---

## 📝 Notas de Diseño

*   **Idempotencia:** Los consumidores están diseñados para procesar eventos de forma idempotente (el uso de IDs fijos en proyecciones ayuda a esto).
*   **Consistencia Eventual:** El saldo en `ms-transactions` es "eventualmente consistente" con `ms-accounts`. Para mitigar riesgos de doble gasto en alta concurrencia, `ms-transactions` aplica una actualización optimista local antes de emitir el evento.
*   **Manejo de Errores:** En un entorno productivo, se implementarían *Dead Letter Queues (DLQ)* para manejar eventos que no se pudieron procesar y patrones de compensación (Saga Pattern) para revertir transacciones fallidas.

---
