import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;


File targetFolder = new File(basedir, "target");
if (!targetFolder.exists()) {
    throw new FileNotFoundException("Folder should exist: " + targetFolder);
}
if (targetFolder.isFile()) {
    throw new FileNotFoundException("Folder should be a folder: " + targetFolder);
}
File classesFolder = new File(targetFolder, "classes");
if (!classesFolder.exists()) {
     throw new FileNotFoundException("Folder should exist: " + classesFolder);  
}
File barClassFile = new File(classesFolder, "org/foo/Bar.class");
if (!barClassFile.exists()) {
    throw new FileNotFoundException("File should exist: " + barClassFile);
} 
int amountOfBytes = Files.readAllBytes(barClassFile.toPath()).length;

File fooClassFile = new File(classesFolder, "org/foo/Foo.class");
if (!fooClassFile.exists()) {
    throw new FileNotFoundException("File should exist: " + fooClassFile);
}

File buildLog = new File(basedir, "build.log");
if (!buildLog.exists()) {
    throw new FileNotFoundException("File should exist: " + buildLog);
}
List<String> listOfStrings = new ArrayList<String>();
listOfStrings = Files.readAllLines(buildLog.toPath());
assert listOfStrings.contains("[DEBUG] Configuring mojo execution 'org.hibernate.orm:hibernate-maven-plugin:0.0.1-SNAPSHOT:enhance:enhance' with basic configurator -->");
assert listOfStrings.contains("[DEBUG]   (f) classesDirectory = " + classesFolder);
assert listOfStrings.contains("[DEBUG]   (f) enableAssociationManagement = false");
assert listOfStrings.contains("[DEBUG]   (f) enableDirtyTracking = false");
assert listOfStrings.contains("[DEBUG]   (f) enableLazyInitialization = true");
assert listOfStrings.contains("[DEBUG]   (f) enableExtendedEnhancement = false");
assert listOfStrings.contains("[DEBUG] Starting execution of enhance mojo");
assert listOfStrings.contains("[DEBUG] Starting assembly of the source set");
assert listOfStrings.contains("[INFO] Added file to source set: " + barClassFile);
assert listOfStrings.contains("[DEBUG] Ending the assembly of the source set");
assert listOfStrings.contains("[DEBUG] Creating bytecode enhancer");
assert listOfStrings.contains("[DEBUG] Creating enhancement context");
assert listOfStrings.contains("[DEBUG] Creating URL ClassLoader for folder: " + classesFolder);
assert listOfStrings.contains("[DEBUG] Starting type discovery");
assert listOfStrings.contains("[DEBUG] Trying to discover types for classes in file: " + barClassFile);
assert listOfStrings.contains("[DEBUG] Determining class name for file: " + barClassFile);
assert listOfStrings.contains("[INFO] Succesfully discovered types for classes in file: " + barClassFile);
assert listOfStrings.contains("[DEBUG] Trying to discover types for classes in file: " + fooClassFile);
assert listOfStrings.contains("[DEBUG] Determining class name for file: " + fooClassFile);
assert listOfStrings.contains("[INFO] Succesfully discovered types for classes in file: " + fooClassFile);
assert listOfStrings.contains("[DEBUG] Ending type discovery");
assert listOfStrings.contains("[DEBUG] Starting class enhancement");
assert listOfStrings.contains("[DEBUG] Trying to enhance class file: " + barClassFile);
assert listOfStrings.contains("[INFO] Succesfully cleared the contents of file: " + barClassFile);
assert listOfStrings.contains("[DEBUG] " + amountOfBytes + " bytes were succesfully written to file: " +barClassFile);
assert listOfStrings.contains("[INFO] Succesfully enhanced class file: " + barClassFile);
assert listOfStrings.contains("[DEBUG] Trying to enhance class file: " + fooClassFile);
assert listOfStrings.contains("[INFO] Skipping file: " + fooClassFile);
assert listOfStrings.contains("[DEBUG] Ending class enhancement");
assert listOfStrings.contains("[DEBUG] Ending execution of enhance mojo");
