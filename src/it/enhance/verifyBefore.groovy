import java.io.*;

File targetFolder = new File(basedir, "target");
if (targetFolder.exists()) {
    throw new FileNotFoundException("Folder should not exist: " + targetFolder);
}
File touchFile = new File(targetFolder, "touch.txt");
if (touchFile.exists()) {
    throw new FileNotFoundException("File should not exist: " + touchFile);
}
