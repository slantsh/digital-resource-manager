# Digital Resource Manager - Codebase Explanation

This document provides a beginner-friendly explanation of every important part of the **Digital Resource Manager** codebase, along with code snippets, concepts, and reasons behind design choices.

---

## Table of Contents
1. [Project Overview & Architecture](#1-project-overview--architecture)
2. [Data Model Layer](#2-data-model-layer)
   - [ResourceType Enum](#resourcetypejava)
   - [Category Class](#categoryjava)
   - [Resource Class](#resourcejava)
3. [Database & JDBC Connection Layer](#3-database--jdbc-connection-layer)
   - [Database URL & Connection](#establishing-a-connection)
   - [Creating Tables & SQL DDL](#creating-tables--sql-ddl)
   - [Enabling SQLite Foreign Keys](#enabling-sqlite-foreign-keys)
   - [Closing Resources & Exception Handling](#closing-resources)
4. [Application Entry Point (Main.java)](#4-application-entry-point-mainjava)
5. [How Compilation and Execution Work](#5-how-compilation-and-execution-work)

---

## 1. Project Overview & Architecture

The project is a beginner-level Java backend that connects directly to a **SQLite** database using **JDBC** (Java Database Connectivity) without any frameworks, Maven, or complex abstractions.

### Directory Structure
```
digital-resource-manager/
├── lib/
│   ├── sqlite-jdbc.jar        # SQLite JDBC driver
│   ├── slf4j-api-1.7.36.jar   # Logging dependency required by driver
│   └── slf4j-nop-1.7.36.jar   # Silences driver log output
├── resources.db               # SQLite database file (created on run)
├── bin/                       # Compiled .class files
└── src/
    └── main/
        └── java/
            ├── Main.java                   # Program entry point
            ├── database/
            │   └── DatabaseConnection.java # JDBC connection & table setup
            └── model/
                ├── ResourceType.java       # Enum for resource types
                ├── Category.java           # Category entity
                └── Resource.java           # Resource entity
```

---

## 2. Data Model Layer

The model classes represent real-world entities in Java memory. They use simple `public` fields (no getters and setters) and straightforward constructors.

### `ResourceType.java`
An **Enum** (short for *Enumeration*) is a special Java type used to define a fixed collection of constants.

```java
package model;

public enum ResourceType {
    PDF,
    IMAGE,
    VIDEO,
    LINK
}
```

#### Why use an Enum here?
- **Type Safety**: Instead of storing the type as a free-form `String` (where someone could accidentally misspell `"pdff"` or `"photo"`), the compiler restricts the values to exactly one of the four allowed constants.
- **Readability**: It clearly communicates the allowed file/resource formats in the domain.

---

### `Category.java`
Represents a group or folder for resources (e.g., "Programming", "Study").

```java
package model;

public class Category {
    public int id;
    public String name;

    // Constructor 1: Used when creating a new category before DB assigns an ID
    public Category(String name) {
        this.name = name;
    }

    // Constructor 2: Used when loading a category that already has an ID in the DB
    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Category [id=" + id + ", name=" + name + "]";
    }
}
```

#### Key Highlights:
1. **Public Fields**: `public int id;` and `public String name;` allow direct access (`category.name`) without boilerplate `getName()` or `setName()` methods.
2. **Two Constructors**:
   - `Category(String name)`: When we create a brand new category in code, we don't know its ID yet because SQLite will automatically generate it.
   - `Category(int id, String name)`: When reading existing records from SQLite, the database already has an `id`.
3. **`toString()`**: Overriding this standard Java method provides a readable string representation when printing with `System.out.println(category)`.

---

### `Resource.java`
Represents a specific digital file or web link.

```java
package model;

public class Resource {
    public int id;
    public String title;
    public String description;
    public ResourceType type;
    public String location;
    public int categoryId;
    public boolean favorite;

    // Constructor without ID (before insertion into database)
    public Resource(String title, String description, ResourceType type, 
                    String location, int categoryId, boolean favorite) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.categoryId = categoryId;
        this.favorite = favorite;
    }

    // Constructor with ID (when fetched from database)
    public Resource(int id, String title, String description, ResourceType type, 
                    String location, int categoryId, boolean favorite) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.categoryId = categoryId;
        this.favorite = favorite;
    }

    @Override
    public String toString() {
        return "Resource [id=" + id 
                + ", title=" + title 
                + ", type=" + type 
                + ", categoryId=" + categoryId 
                + ", favorite=" + favorite 
                + ", location=" + location + "]";
    }
}
```

#### Key Highlights:
- **Relational Link (`categoryId`)**: The field `int categoryId` matches the foreign key column in the database, linking this resource to a specific `Category`.
- **`ResourceType type`**: Uses the enum rather than a raw string.
- **`boolean favorite`**: Java handles this as `true` or `false` (in SQLite, it is represented as `1` or `0`).

---

## 3. Database & JDBC Connection Layer

All database setup is centralized in [`DatabaseConnection.java`](file:///home/slant/college%20dev/digital-resource-manager/src/main/java/database/DatabaseConnection.java).

### Establishing a Connection
```java
private static final String URL = "jdbc:sqlite:resources.db";

public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL);
}
```

#### Line-by-Line Breakdown:
- `"jdbc:sqlite:resources.db"`:
  - `jdbc:` tells Java to use the JDBC protocol.
  - `sqlite:` tells the JDBC driver manager to use the SQLite driver.
  - `resources.db:` the file path of the database on disk. If `resources.db` does not exist, SQLite creates it automatically.
- `DriverManager.getConnection(URL)`: Searches the classpath for a registered driver that understands `jdbc:sqlite:` and opens a connection.

---

### Creating Tables & SQL DDL
```java
String createCategoriesTable = "CREATE TABLE IF NOT EXISTS categories ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "name TEXT NOT NULL UNIQUE"
        + ");";

String createResourcesTable = "CREATE TABLE IF NOT EXISTS resources ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "title TEXT NOT NULL, "
        + "description TEXT, "
        + "type TEXT NOT NULL, "
        + "location TEXT NOT NULL, "
        + "category_id INTEGER, "
        + "favorite INTEGER DEFAULT 0, "
        + "FOREIGN KEY (category_id) REFERENCES categories (id)"
        + ");";
```

#### SQL Concepts Explained:
- `CREATE TABLE IF NOT EXISTS`: Prevents SQL errors if the table was already created on a prior run.
- `INTEGER PRIMARY KEY AUTOINCREMENT`:
  - Every row gets a unique integer ID.
  - SQLite automatically generates and increments this value (`1, 2, 3...`) whenever a new row is added.
- `NOT NULL UNIQUE`: Ensures that `name` cannot be blank and no two categories have the same name.
- `FOREIGN KEY (category_id) REFERENCES categories (id)`: Creates a relational link. Ensures a resource cannot refer to a non-existent category ID.

---

### Enabling SQLite Foreign Keys
```java
Connection conn = getConnection();
Statement stmt = conn.createStatement();

// Enable foreign key support in SQLite
stmt.execute("PRAGMA foreign_keys = ON;");

// Create tables
stmt.execute(createCategoriesTable);
stmt.execute(createResourcesTable);
```

#### Why is `PRAGMA foreign_keys = ON;` needed?
By default, SQLite disables foreign key enforcement for backwards compatibility. Executing this PRAGMA statement per connection instructs SQLite to actively enforce foreign key rules.

---

### Closing Resources
```java
stmt.close();
conn.close();
```

#### Why close?
- Database connections and statements use system file handles and memory.
- If you don't close them, SQLite may keep file locks on `resources.db`, causing `"database is locked"` errors.

---

## 4. Application Entry Point (`Main.java`)

[`Main.java`](file:///home/slant/college%20dev/digital-resource-manager/src/main/java/Main.java) serves as the entry point of the application:

```java
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        System.out.println("Connecting to SQLite database...");

        // 1. Verify that a connection can be established
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                System.out.println("Connected to SQLite successfully!");
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            return;
        }

        // 2. Execute table creation
        System.out.println("Setting up database tables...");
        DatabaseConnection.setupDatabase();
        System.out.println("Database setup complete!");
    }
}
```

### Flow of Execution:
1. `getConnection()` is called to test that the SQLite JDBC driver is present on the classpath and that the database file can be accessed.
2. The connection is immediately closed.
3. `DatabaseConnection.setupDatabase()` is invoked to execute the DDL queries that ensure both `categories` and `resources` tables exist.

---

## 5. How Compilation and Execution Work

Because the project does not use Maven or Gradle, compiling and running is done using standard JDK tools:

### Step 1: Compiling with `javac`
```bash
javac -cp "lib/*" -d bin src/main/java/model/*.java src/main/java/database/*.java src/main/java/Main.java
```
- `-cp "lib/*"` (Classpath): Tells the Java compiler to include all `.jar` files in `lib/` (specifically `sqlite-jdbc.jar` and SLF4J).
- `-d bin`: Puts the compiled `.class` bytecode files into the `bin/` directory.

### Step 2: Running with `java`
```bash
# On Linux / macOS:
java -cp "bin:lib/*" Main

# On Windows (Command Prompt):
java -cp "bin;lib/*" Main
```
- `-cp "bin:lib/*"`: Instructs the JVM to look for compiled classes in `bin/` and external libraries in `lib/`.
- `Main`: Tells the JVM to execute the `main` method in `Main.class`.
