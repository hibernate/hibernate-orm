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
assert listOfStrings.contains("[INFO] Starting 'enhance' mojo execution with the following parameters :");
assert listOfStrings.contains("[INFO]   classesDirectory: " + classesFolder);
assert listOfStrings.contains("[INFO]   enableAssociationManagement: false");


