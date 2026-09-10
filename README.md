# Digital Resource Manager (Database Setup)

A minimal, beginner-level Java project utilizing SQLite and JDBC to connect to a local database and set up tables.

## Project Structure
```
digital-resource-manager/
├── lib/
│   ├── sqlite-jdbc.jar
│   ├── slf4j-api-1.7.36.jar
│   └── slf4j-nop-1.7.36.jar
├── resources.db
├── bin/
└── src/
    └── main/
        └── java/
            ├── Main.java
            ├── model/
            │   ├── Category.java
            │   ├── Resource.java
            │   └── ResourceType.java
            └── database/
                └── DatabaseConnection.java
```

## How to Compile and Run

Compile:
```bash
javac -cp "lib/*" -d bin src/main/java/model/*.java src/main/java/database/*.java src/main/java/Main.java
```

Run:
```bash
java -cp "bin:lib/*" Main
```
*(On Windows, use a semicolon instead of a colon: `java -cp "bin;lib/*" Main`)*
