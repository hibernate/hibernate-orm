import java.io.*;

File targetFolder = new File(basedir, "target");
if (!targetFolder.exists()) {
    throw new FileNotFoundException("Folder should exist: " + targetFolder);
}
if (targetFolder.isFile()) {
    throw new FileNotFoundException("Folder should be a folder: " + targetFolder);
}
File touchFile = new File(targetFolder, "touch.txt");
if (!touchFile.exists()) {
    throw new FileNotFoundException("File should exist: " + touchFile);
}
File classesFolder = new File(targetFolder, "classes");
if (!classesFolder.exists()) {
     throw new FileNotFoundException("Folder should exist: " + classesFolder);  
}
File barClassFile = new File(classesFolder, "org/foo/Bar.class");
if (!barClassFile.exists()) {
    throw new FileNotFoundException("File should exist: " + barClassFile);
}
