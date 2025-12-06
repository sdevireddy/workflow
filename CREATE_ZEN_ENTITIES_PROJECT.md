# Create zen-entities Maven Project

## Problem
- workflow-service can't find WorkflowExecution, WorkflowExecutionLog, etc.
- These entities should be in zen-entities JAR
- zen-entities is a separate Maven project (not part of notify)

## Solution: Create zen-entities Project

### Step 1: Create Project Structure

```
C:\zen-entities\
├── pom.xml
└── src\main\java\com\zen\entities\
    ├── common\
    └── tenant\
        ├── Workflow.java
        ├── WorkflowExecution.java
        ├── WorkflowExecutionLog.java
        ├── WorkflowNode.java
        └── WorkflowApprovalRequest.java
```

### Step 2: Create pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.zen</groupId>
    <artifactId>zen-entities</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>Zen Entities</name>
    <description>Shared entity classes for all Zen microservices</description>
    
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>
    
    <dependencies>
        <!-- Jakarta Persistence API -->
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
            <version>3.1.0</version>
        </dependency>
        
        <!-- Lombok (optional but recommended) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 3: Copy Entity Files

Copy these files from notify to zen-entities:
- `notify/src/main/java/com/zen/entities/tenant/Workflow.java` → `zen-entities/src/main/java/com/zen/entities/tenant/Workflow.java`
- `notify/src/main/java/com/zen/entities/tenant/WorkflowExecution.java` → `zen-entities/src/main/java/com/zen/entities/tenant/WorkflowExecution.java`
- `notify/src/main/java/com/zen/entities/tenant/WorkflowExecutionLog.java` → `zen-entities/src/main/java/com/zen/entities/tenant/WorkflowExecutionLog.java`
- `notify/src/main/java/com/zen/entities/tenant/WorkflowNode.java` → `zen-entities/src/main/java/com/zen/entities/tenant/WorkflowNode.java`
- `notify/src/main/java/com/zen/entities/tenant/WorkflowApprovalRequest.java` → `zen-entities/src/main/java/com/zen/entities/tenant/WorkflowApprovalRequest.java`

### Step 4: Build and Install

```cmd
cd C:\zen-entities
mvn clean install
```

This will:
- Compile all entity classes
- Package them into zen-entities-1.0.0.jar
- Install to Maven local repository (~/.m2/repository/com/zen/zen-entities/1.0.0/)

### Step 5: Rebuild workflow-service

```cmd
cd C:\workflow-service
mvn clean compile
```

## Quick Setup Script

Create `C:\setup-zen-entities.bat`:

```batch
@echo off
echo Creating zen-entities project...

mkdir C:\zen-entities
mkdir C:\zen-entities\src\main\java\com\zen\entities\common
mkdir C:\zen-entities\src\main\java\com\zen\entities\tenant

echo Copying entity files...
copy C:\notify3\notify\src\main\java\com\zen\entities\tenant\*.java C:\zen-entities\src\main\java\com\zen\entities\tenant\

echo Creating pom.xml...
(
echo ^<?xml version="1.0" encoding="UTF-8"?^>
echo ^<project xmlns="http://maven.apache.org/POM/4.0.0"
echo          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
echo          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
echo          https://maven.apache.org/xsd/maven-4.0.0.xsd"^>
echo     ^<modelVersion^>4.0.0^</modelVersion^>
echo     ^<groupId^>com.zen^</groupId^>
echo     ^<artifactId^>zen-entities^</artifactId^>
echo     ^<version^>1.0.0^</version^>
echo     ^<packaging^>jar^</packaging^>
echo     ^<properties^>
echo         ^<java.version^>17^</java.version^>
echo         ^<maven.compiler.source^>17^</maven.compiler.source^>
echo         ^<maven.compiler.target^>17^</maven.compiler.target^>
echo     ^</properties^>
echo     ^<dependencies^>
echo         ^<dependency^>
echo             ^<groupId^>jakarta.persistence^</groupId^>
echo             ^<artifactId^>jakarta.persistence-api^</artifactId^>
echo             ^<version^>3.1.0^</version^>
echo         ^</dependency^>
echo     ^</dependencies^>
echo ^</project^>
) > C:\zen-entities\pom.xml

echo Building zen-entities...
cd C:\zen-entities
call mvn clean install

echo Done!
pause
```

## Alternative: Where is your existing zen-entities?

If zen-entities already exists somewhere, find it and add the workflow entities there:

```cmd
# Search for existing zen-entities
dir /s /b C:\*zen-entities*.jar

# Or search in Maven local repo
dir /s /b %USERPROFILE%\.m2\repository\com\zen\zen-entities
```

Then add the workflow entities to that project's source code.
