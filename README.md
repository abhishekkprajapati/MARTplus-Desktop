# MARTplus Desktop Application

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Build-Maven%203.9+-C71A36.svg)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL%20JDBC-336791.svg)](https://jdbc.postgresql.org/)
[![UI-FlatLaf](https://img.shields.io/badge/UI-FlatLaf%20Modern%20Swing-635BFF.svg)](https://www.formdev.com/flatlaf/)
[![Tests](https://img.shields.io/badge/Tests-36%20Passed-brightgreen.svg)]()

A modern, standalone **Java Swing desktop e-commerce platform** with direct **PostgreSQL JDBC database connectivity**, multi-vendor role-based access control (Buyer, Seller, Admin), ACID transaction management, and an asynchronous, thread-safe UI architecture.

---

## 🚀 Key Features

- **Direct JDBC Architecture:** Zero JPA/Hibernate dependencies. Native parameterized `PreparedStatement`, `ResultSet`, and explicit ACID transaction management (`conn.setAutoCommit(false)`, `conn.commit()`, `conn.rollback()`).
- **Modern E-Commerce UI:** Built with Java Swing and FlatLaf Light Look & Feel matching the MARTplus modern design system (`#635BFF` primary).
- **Two-Column Product Details Showcase:** High-resolution product images with aspect-ratio preserving scaling, dynamic metadata hierarchy, non-editable sanitized descriptions, interactive quantity stepper, live subtotal calculator, and stock boundary protection.
- **Role-Based Portals:**
  - 🛍️ **Buyer Portal:** Product browsing with search & category filters, shopping cart, saved wishlist, order history & status tracking, shipping addresses, and profile management.
  - 🏪 **Seller Portal:** Product catalog management, live inventory tracking, low-stock alerts, and order fulfillment.
  - 🛡️ **Admin Portal:** System-wide user accounts management, catalog moderation, orders oversight, and activity audit trails.
- **Multithreading & Synchronization:**
  - `AsyncOperation<T>` decouples heavy database queries onto background threads, dispatching results to the Event Dispatch Thread (EDT) via `SwingUtilities.invokeLater()`.
  - `InventoryLockManager` employs fine-grained `ReentrantLock` striping via `ConcurrentHashMap<Long, ReentrantLock>` to prevent race conditions and overselling during simultaneous checkouts.
- **Security & Authentication:**
  - Complete in-app registration (Buyer / Seller) and login.
  - Secure BCrypt password hashing (Cost factor 10) matching web authentication standards.

---

## 🛠️ Technology Stack

| Component | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java (JDK 21 LTS) | 21.0+ |
| **GUI Framework** | Java Swing + FlatLaf | 3.4.1 |
| **Persistence** | Direct JDBC (`java.sql.*`) | PostgreSQL Driver 42.7.3 |
| **Security** | jBCrypt | 0.4 |
| **Testing** | JUnit 5 Jupiter + Mockito | 5.10.2 / 5.11.0 |
| **Build Tool** | Apache Maven | 3.8+ |

---

## 📂 Project Structure

```text
MARTplus-Desktop/
├── pom.xml                                    # Standalone Maven configuration
├── README.md                                  # Project documentation & run guide
├── .env.example                               # Environment credentials template
├── .gitignore                                 # Git ignore rules
│
├── src/
│   ├── main/
│   │   ├── java/com/martplus/desktop/
│   │   │   ├── Main.java                      # Desktop Application Entry Point
│   │   │   ├── config/                        # Database & Application Session Config
│   │   │   │   ├── AppConfig.java
│   │   │   │   └── DatabaseConfig.java        # Direct JDBC & .env loader
│   │   │   ├── model/                         # 11 Domain POJO Entities
│   │   │   ├── dao/                           # GenericDao & 9 Specialized JDBC DAOs
│   │   │   ├── service/                       # 8 Domain Business Services
│   │   │   ├── controller/                    # MVC Controllers
│   │   │   ├── concurrency/                   # AsyncOperation & InventoryLockManager
│   │   │   ├── exception/                     # Custom Exception Hierarchy
│   │   │   ├── ui/                            # Swing Panels, Frames & Dialogs
│   │   │   └── util/                          # UITheme, ImageUtil & ValidationUtil
│   │   │
│   │   └── resources/                         # Brand icons & UI assets
│   │       └── icons/
│   │           ├── favicon.png
│   │           └── mplus-logo.png
│   │
│   └── test/
│       ├── java/com/martplus/desktop/         # 10 Test Classes (36 Automated Tests)
│       └── resources/
│           └── mockito-extensions/
│
└── docs/                                      # Academic Assessment Documentation
    ├── class-diagram.md
    ├── collections-generics.md
    ├── database-er-diagram.md
    ├── jdbc-implementation.md
    ├── multithreading-synchronization.md
    ├── oop-implementation.md
    ├── project-overview.md
    ├── system-architecture.md
    ├── transaction-management.md
    ├── use-case-diagram.md
    └── viva-questions.md
```

---

## ⚙️ Setup and Installation

### 1. Prerequisites
- **JDK 21** or later installed (`java -version`).
- **Apache Maven 3.8+** installed (`mvn -version`).
- **PostgreSQL 14+** running locally or accessible remotely.

### 2. Configure Database Credentials
Create a local `.env` file by copying the example:
```bash
cp .env.example .env
```
Edit `.env` with your PostgreSQL connection parameters:
```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/shopsphere
DATABASE_USERNAME=shopsphere_user
DATABASE_PASSWORD=your_local_password
```

### 3. Build & Run Tests
Execute the independent Maven build and test verification:
```bash
mvn clean verify
```
Expected output:
```
[INFO] Results: Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 4. Launch the Desktop Application
Start the Java Swing application:
```bash
mvn exec:java
```

---

## 🧪 Testing Verification Matrix

The test suite validates all core academic rubric requirements without requiring an active external database daemon:

- `CheckoutTransactionTest`: Atomic checkout, stock decrement, order header/items persistence, cart clearance, and rollback verification.
- `StockValidationTest`: Negative quantity rejection, out-of-stock reservation prevention.
- `InventoryLockManagerTest`: High-concurrency race condition prevention using ReentrantLock striping.
- `ProductDetailsUITest`: Aspect-ratio-preserving image scaling and description deduplication.
- `UserRegistrationTest`: 9 comprehensive registration scenarios (Buyer, Seller, duplicate checks, password validation, ADMIN blocking).
- `GenericDaoTest`: Generic repository contract tests.
- `PasswordUtilTest`: BCrypt hashing and verification.
- `RoleDashboardFactoryTest`: Polymorphic dashboard generation.
- `DataIsolationTest`: Multi-vendor role boundary enforcement.

---

## 📄 License & Attribution

MARTplus Desktop is an independent Java GUI application developed for academic evaluation and multi-vendor e-commerce enterprise architecture.
