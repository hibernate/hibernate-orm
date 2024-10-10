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
assert listOfStrings.contains("[DEBUG]   (f) enableLazyInitialization = false");


