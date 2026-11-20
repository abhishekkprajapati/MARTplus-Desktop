# MARTplus — Project Overview & Academic Review 1 Compliance

## 1. Project Background
**MARTplus** is an enterprise-grade multi-vendor e-commerce platform. While the production web environment utilizes a modern Spring Boot + PostgreSQL web architecture, academic evaluation under the **Java GUI-Based Projects Marking Rubric** necessitates an independent native desktop application built with:
- Pure **Java 21 Swing** (FlatLaf Modern Look & Feel)
- Direct **PostgreSQL JDBC Database Connectivity** (no ORM or Spring Data abstraction)
- Robust **Core Java OOP Principles** (Encapsulation, Inheritance, Polymorphism, Interfaces, Custom Exception Hierarchy)
- Real **Java Collections & Generics** (Generic DAO interface, type-safe lists, maps, and concurrent sets)
- Real **Multithreading & Thread Synchronization** (`AsyncOperation` SwingWorker, `BackgroundTaskManager`, `InventoryLockManager` ReentrantLock)
- Atomic **Multi-step JDBC Transactions** (`setAutoCommit(false)`, `commit()`, `rollback()`)

This desktop application operates as an independent, standalone Maven project in `MARTplus-Desktop`, sharing the existing PostgreSQL database schema while maintaining 100% decoupling from the web backend and frontend.

---

## 2. Rubric Mapping Summary

| Review 1 Assessment Rubric | Mark Weight | Primary Implementation Packages & Classes | Verification Tests |
| :--- | :---: | :--- | :--- |
| **OOP Implementation** (Polymorphism, Inheritance, Exception Handling, Interfaces) | **10 Marks** | `BaseDashboard`, `DashboardView`, `RoleDashboardFactory`, `GenericDao<T,ID>`, `ApplicationException` hierarchy | `RoleDashboardFactoryTest`, `GenericDaoTest`, `ValidationUtilTest` |
| **Collections & Generics** | **6 Marks** | `GenericDao<T,ID>`, `AbstractJdbcDao<T,ID>`, `List<Product>`, `Set<Long>`, `Map<Long,Product>`, `List<CartItem>` | `GenericDaoTest`, `StockValidationTest` |
| **Multithreading & Synchronization** | **4 Marks** | `BackgroundTaskManager` (ThreadPoolExecutor), `InventoryLockManager` (`ReentrantLock`, `ConcurrentHashMap`), `AsyncOperation` | `InventoryLockManagerTest` |
| **Classes for Database Operations** | **7 Marks** | `AbstractJdbcDao`, `JdbcUserDao`, `JdbcProductDao`, `JdbcCartDao`, `JdbcOrderDao`, `JdbcInventoryDao`, `JdbcAddressDao`, `JdbcActivityLogDao` | `DataIsolationTest`, `CheckoutTransactionTest`, `UserRegistrationTest` |
| **Database Connectivity (JDBC)** | **3 Marks** | `DatabaseConfig`, `DriverManager.getConnection()`, `PreparedStatement`, `ResultSet`, try-with-resources | `PasswordUtilTest`, `CheckoutTransactionTest` |
| **Implement JDBC Connectivity** (Transaction Management) | **3 Marks** | `OrderService.processCheckout`: atomic checkout (`setAutoCommit(false)`, `commit()`, `rollback()`, `SELECT FOR UPDATE`) | `CheckoutTransactionTest` |
| **Total Review 1 Scope** | **33 Marks** | **All 6 Criteria 100% Implemented & Tested** | **36 Tests Passing (0 Failures)** |

---

## 3. Brand Identity & Swing Design System
- **Desktop Window Dimensions:** Default $1280 \times 820$, minimum $1024 \times 680$, fully responsive.
- **Brand Colors (Website Aligned):**
  - Primary Purple: `#635BFF`
  - Hover Primary: `#5148E5`
  - Active Pill Background: `#EEEDFF`
  - Clean Background: `#F8F9FC`
  - White Cards: `#FFFFFF` with rounded borders (`#E5E7EB`)
  - Dark Headings: `#111827`
  - Muted Text: `#6B7280`
  - Success Green: `#16A34A`
  - Danger Red: `#EF4444`
- **Typography:** Inter / System Sans-Serif hierarchy (`FONT_TITLE`, `FONT_SUBTITLE`, `FONT_HEADER`, `FONT_BODY`, `FONT_BODY_BOLD`, `FONT_SMALL`).
- **UI Safety:** Heavy database reads/writes run on dedicated background threads via `AsyncOperation`, keeping the Swing Event Dispatch Thread (EDT) completely fluid.
