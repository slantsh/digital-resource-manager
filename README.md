# Digital Resource Manager

A clean, modern Java Swing desktop application utilizing SQLite and JDBC to store, organize, and manage digital resources and files in one place.

---

## 🎯 Purpose of the Project
- **Simplify access** to digital resources (documents, images, videos, and web links).
- **Keep files structured and easy to find** with categories, live search, and favorites.
- **Maintain resource data efficiently** with local SQLite database persistence.
- **Seamless Drag-and-Drop** import for single or multiple files from your desktop / file manager or URLs from your browser.

---

## 📦 Supported Resource Types
- **PDF** – documents and reading materials (`.pdf`)
- **Image** – photos and visual content (`.png`, `.jpg`, `.jpeg`, `.gif`, `.webp`, `.svg`, etc.)
- **Video** – multimedia files (`.mp4`, `.mkv`, `.avi`, `.mov`, `.webm`, etc.)
- **Link** – web-based resources and bookmarks (`https://...`, `http://...`)

---

## 🖥️ Features
- **Clean Swing UI**: Native Look & Feel without bloated themes or unnecessary styling hacks.
- **Drag & Drop Import**: Drop files directly onto the drop zone or table to instantly catalog them with automatic file type detection. Drop web URLs to quickly save links.
- **Collections & Categories**: Organize resources by custom categories with live count badges, with options to create, rename, and delete categories.
- **Search & Filter**: Real-time search query filtering (title, description, path) alongside Resource Type and Favorites filtering.
- **Quick Open & Reveal**: Double-click any row to open the file in its default OS viewer or launch web links in your browser. Right-click to show the containing folder on disk.
- **Favorites (★)**: One-click favorite toggling directly from the table or context menu.

---

## 📷 Screenshots
Screenshots are captured in the [`screenshots/`](screenshots/) directory:

| Screenshot | Description |
|------------|-------------|
| [`01_main_window.png`](screenshots/01_main_window.png) | Main dashboard with collections sidebar, drop zone, and resource table with type badges |
| [`02_add_resource_dialog.png`](screenshots/02_add_resource_dialog.png) | Add / Edit Resource modal dialog with file picker and category selector |
| [`03_filtered_view.png`](screenshots/03_filtered_view.png) | Search & filter view with real-time query results |
| [`04_about_dialog.png`](screenshots/04_about_dialog.png) | About dialog presenting the application's goals and supported formats |

---

## 📁 Project Structure
```
digital-resource-manager/
├── lib/
│   ├── sqlite-jdbc.jar        # SQLite JDBC driver
│   ├── slf4j-api-1.7.36.jar   # Logging API
│   └── slf4j-nop-1.7.36.jar   # Log silencer
├── resources.db               # SQLite database file
├── bin/                       # Compiled .class files
└── src/
    └── main/
        └── java/
            ├── Main.java                   # Program entry point & GUI launcher
            ├── model/
            │   ├── Category.java           # Category entity
            │   ├── Resource.java           # Resource entity
            │   └── ResourceType.java       # ResourceType enum with auto-detection
            ├── database/
            │   ├── DatabaseConnection.java # SQLite connection & schema initialization
            │   ├── CategoryDAO.java        # Category CRUD & counts
            │   └── ResourceDAO.java        # Resource CRUD & search/filter queries
            └── ui/
                ├── MainFrame.java          # Main window, toolbar, sidebar & table
                ├── DropZonePanel.java      # Visual drop area with click-to-browse
                ├── DropTargetHandler.java  # Drag-and-drop listener for files & URLs
                ├── ResourceDialog.java     # Add / Edit resource dialog
                ├── ResourceTableModel.java # AbstractTableModel for JTable
                ├── TableRenderers.java     # Clean table cell renderers (★, Badges)
                └── CategoryItem.java       # Category sidebar list items
```

---

## 🚀 How to Compile and Run

### Quick Run (Bash Script):
Run the provided automated build and run script:
```bash
./run.sh
```

### Manual Compile & Run (Auto-Build via `-sourcepath`):
Using the `-sourcepath` flag, `javac` automatically traces and compiles all dependent classes starting from just `Main.java`:

#### Linux / macOS:
```bash
# Auto-compile from Main.java
javac -cp "lib/*" -sourcepath src/main/java -d bin src/main/java/Main.java

# Run
java -cp "bin:lib/*" Main
```

#### Windows:
```cmd
javac -cp "lib/*" -sourcepath src/main/java -d bin src/main/java/Main.java
java -cp "bin;lib/*" Main
```
