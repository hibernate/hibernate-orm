import java.io.*;

File outputDirectory = new File(basedir, "outputDirectory");
if (outputDirectory.exists()) {
    throw new FileNotFoundException("Folder should not exist: " + outputDirectory);
}
File targetFolder = new File(basedir, "targetFolder");
if (targetFolder.exists()) {
    throw new FileNotFoundException("Folder should not exist: " + targetFolder);
}
File touchFile = new File(targetFolder, "touch.txt");
if (touchFile.exists()) {
    throw new FileNotFoundException("File should not exist: " + touchFile);
}
