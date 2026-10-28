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
4. [Data Access Object (DAO) Layer](#4-data-access-object-dao-layer)
   - [CategoryDAO](#categorydaojava)
   - [ResourceDAO](#resourcedaojava)
5. [User Interface Layer (Swing UI)](#5-user-interface-layer-swing-ui)
   - [Native Look & Feel Strategy](#native-look--feel-strategy)
   - [Window & Layout Architecture (MainFrame.java)](#window--layout-architecture)
   - [Resource Table & Custom Renderers](#resource-table--custom-renderers)
   - [Resource Dialog](#resourcedialogjava)
6. [Drag and Drop Subsystem](#6-drag-and-drop-subsystem)
   - [DropTargetHandler](#droptargethandlerjava)
   - [DropZonePanel](#dropzonepaneljava)
   - [Automatic Resource Type & Metadata Detection](#automatic-resource-type-detection)
7. [Desktop Integration (Open & Browse)](#7-desktop-integration-open--browse)
8. [Application Entry Point (Main.java)](#8-application-entry-point-mainjava)
9. [How Compilation and Execution Work](#9-how-compilation-and-execution-work)

---

## 1. Project Overview & Architecture

The project is a complete Java desktop application that pairs a clean **Swing UI** with a **SQLite** database using **JDBC** (Java Database Connectivity) without bloated frameworks or third-party theme engines.

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
            ├── Main.java                   # Program entry point & GUI launcher
            ├── database/
            │   ├── DatabaseConnection.java # JDBC connection & table setup
            │   ├── CategoryDAO.java        # Category CRUD & counts
            │   └── ResourceDAO.java        # Resource CRUD & search queries
            ├── model/
            │   ├── ResourceType.java       # Enum for resource types + auto-detection
            │   ├── Category.java           # Category entity
            │   └── Resource.java           # Resource entity
            └── ui/
                ├── MainFrame.java          # Main window with sidebar, table & search
                ├── DropZonePanel.java      # Visual drop area with click-to-browse
                ├── DropTargetHandler.java  # Drag-and-drop adapter for files & URLs
                ├── ResourceDialog.java     # Add / Edit resource modal dialog
                ├── ResourceTableModel.java # AbstractTableModel for JTable
                ├── TableRenderers.java     # Clean table cell renderers (★, Badges)
                └── CategoryItem.java       # Category sidebar list items
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

## 4. Data Access Object (DAO) Layer

To keep database code clean and separated from UI logic, the project uses the **DAO (Data Access Object) Pattern**.

### `CategoryDAO.java`
Handles querying, inserting, renaming, and deleting categories:
- `getAllCategories()`: Fetches all categories ordered alphabetically.
- `addCategory(name)`: Inserts a new category and retrieves its auto-generated primary key using `Statement.RETURN_GENERATED_KEYS`.
- `updateCategory(id, newName)`: Updates a category's name.
- `deleteCategory(id)`: Safely reassigns resources to a fallback category (or deletes them) before removing the category, preventing foreign key violations.
- `getResourceCounts()`: Aggregates resource counts per category using `GROUP BY category_id` for display on the sidebar.

### `ResourceDAO.java`
Handles all resource retrieval and mutation queries:
- `filterResources(categoryId, type, query, favoritesOnly)`: Dynamically constructs a parameterized SQL query with `PreparedStatement` to support:
  - Filtering by category
  - Filtering by `ResourceType` (PDF, IMAGE, VIDEO, LINK)
  - Full-text search across `title`, `description`, and `location` using `LIKE %query%`
  - Filtering by favorite flag (`favorite = 1`)
- `addResource(resource)`: Inserts a resource and assigns the newly generated ID.
- `updateResource(resource)`: Updates title, description, location, type, category, and favorite status.
- `deleteResource(id)`: Removes the resource from SQLite.
- `setFavorite(id, favorite)`: Quickly toggles the favorite state.

---

## 5. User Interface Layer (Swing UI)

The user interface is built using Java's built-in **Swing** toolkit (`javax.swing.*`), avoiding heavy or unstable third-party look-and-feel libraries while maintaining a modern, native aesthetic.

### Native Look & Feel Strategy
The UI avoids garish styling hacks by requesting the system's native look and feel:
```java
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
```
This ensures the window, buttons, fonts, and borders match the user's operating system (Windows, macOS, or Linux).

### Window & Layout Architecture (`MainFrame.java`)
- **Menu Bar**: Standard `File`, `Edit`, `View`, and `Help` menus with common keyboard shortcuts (`Ctrl+N`, `Ctrl+O`, `Ctrl+E`, `Delete`, `F5`).
- **Top Toolbar**: Contains quick-action buttons ("➕ Add Resource", "📂 Import Files"), real-time search field with live document listener, resource type filter, and favorites toggle.
- **Split View (`JSplitPane`)**:
  - **Left Sidebar**: A `JList` of categories featuring item counts and quick-filter options ("📁 All Resources", "⭐ Favorites", plus custom categories). Below the list are buttons to create, rename, or delete categories.
  - **Right Main Panel**: Contains the `DropZonePanel` at the top and the `JTable` resource list in the center.
- **Status Bar**: Displays total vs. visible resource counts and real-time activity feedback (e.g., "Imported 3 file(s)", "Opened in browser").

### Resource Table & Custom Renderers
The `JTable` is powered by `ResourceTableModel` (`AbstractTableModel`):
- **Columns**: `Fav` (★), `Type`, `Title`, `Category`, `Location`.
- **Inline Editing**: Clicking the star icon directly in the table immediately toggles the favorite status in the database.
- **Table Renderers (`TableRenderers.java`)**:
  - `FavoriteRenderer`: Renders a bright golden star `★` when marked as favorite or dim `☆` when not.
  - `TypeBadgeRenderer`: Color-codes the resource type badge (e.g. Red for PDF, Green for IMAGE, Purple for VIDEO, Blue for LINK) for rapid visual scanning.
  - `PaddedTextRenderer`: Adds comfortable padding to text cells for readability.
- **Context Menu & Double Click**:
  - Double-clicking a row immediately opens the file or URL.
  - Right-clicking opens a context menu with actions: *Open Resource*, *Show Containing Folder*, *Toggle Favorite*, *Edit Details*, *Copy Path / URL*, and *Delete*.

### Resource Dialog (`ResourceDialog.java`)
A modal dialog for adding or editing resources:
- Provides fields for Title, Type, Category, Location, Description, and Favorite.
- Includes a **"Browse..."** button opening `JFileChooser`.
- When a file is chosen, it automatically derives the resource title from the filename (stripping extensions) and auto-selects the matching `ResourceType`.

---

## 6. Drag and Drop Subsystem

The application features full drag-and-drop capabilities powered by Java AWT DnD (`java.awt.dnd.*` and `java.awt.datatransfer.*`).

### `DropTargetHandler.java`
Implements `DropTargetAdapter` to listen for drag and drop events:
1. `dragEnter` & `dragOver`: Checks if the transferable object contains `DataFlavor.javaFileListFlavor` (files from OS file manager) or `DataFlavor.stringFlavor` (URLs from web browsers).
2. `drop`: Extracts the dropped `List<File>` or URL string and delegates to the main frame.

### `DropZonePanel.java`
A dedicated drop target displayed prominently above the table:
- Features a clean dashed border and subtle hover feedback on drag-over.
- Displays helpful instructions: *"Drag & drop files or web links here to add them (or click to browse)"*.
- Clicking anywhere on the panel opens the file chooser for manual import.
- Drag-and-drop is also registered globally on the `JTable` and main window so users can drop files anywhere.

### Automatic Resource Type Detection
`ResourceType.fromFileName(pathOrUrl)` parses extensions or protocols:
- `.pdf` -> `ResourceType.PDF`
- `.png`, `.jpg`, `.jpeg`, `.gif`, `.webp`, `.svg`, `.bmp` -> `ResourceType.IMAGE`
- `.mp4`, `.mkv`, `.avi`, `.mov`, `.webm`, `.flv` -> `ResourceType.VIDEO`
- `http://`, `https://`, `.url`, `.html` -> `ResourceType.LINK`

---

## 7. Desktop Integration (Open & Browse)

Using `java.awt.Desktop`, the manager integrates directly with the operating system:
- **Web Links**: Uses `Desktop.getDesktop().browse(new URI(location))` to open URLs in the user's default web browser.
- **Local Files**: Uses `Desktop.getDesktop().open(new File(location))` to launch documents, images, and videos in their respective OS viewers.
- **Show in Folder**: Uses `Desktop.getDesktop().open(file.getParentFile())` to reveal the containing folder directly in the system file explorer.

---

## 8. Application Entry Point (`Main.java`)

[`Main.java`](file:///home/slant/college%20dev/digital-resource-manager/src/main/java/Main.java) initializes the environment and launches the GUI:

```java
public class Main {
    public static void main(String[] args) {
        // 1. Verify connection and setup SQLite tables
        DatabaseConnection.setupDatabase();

        // 2. Check for headless display environments
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Running in headless mode. GUI cannot be displayed.");
            return;
        }

        // 3. Launch Swing GUI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
```

---

## 9. How Compilation and Execution Work

The project builds cleanly with standard JDK tools without external build system requirements.

### Quick Start with the Bash Script (`run.sh`)
An executable bash script is provided at the root of the project:
```bash
./run.sh
```

### Auto-Building from `Main.java` using `-sourcepath`
Instead of listing every individual `.java` file, you can point `javac` to just `Main.java` and provide the `-sourcepath` flag. The Java compiler will automatically inspect all imported classes (`database.*`, `model.*`, `ui.*`), locate their source files in `src/main/java`, and compile everything in the correct dependency order:

```bash
# Compile everything automatically starting from Main.java:
javac -cp "lib/*" -sourcepath src/main/java -d bin src/main/java/Main.java

# Run:
# On Linux / macOS:
java -cp "bin:lib/*" Main

# On Windows:
java -cp "bin;lib/*" Main
```

#### Why `-sourcepath` Works:
- `-sourcepath src/main/java`: Informs `javac` where the root of the source packages is located.
- When `Main.java` references `database.DatabaseConnection` or `ui.MainFrame`, `javac` resolves their source files automatically at `src/main/java/database/DatabaseConnection.java` and `src/main/java/ui/MainFrame.java`, recursively pulling in all sub-dependencies (`ResourceDAO`, `CategoryDAO`, `ResourceType`, etc.).
