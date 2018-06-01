package org.hibernate.tool.internal.export.doc;

import java.io.File;
import java.util.List;

/**
 * Represents a documentation file.
 * 
 * @author Ricardo C. Moral
 */
public class DocFile {

    /**
     * The name of the file.
     */
    final private String name;

    /**
     * The parent folder.
     */
    final private DocFolder parentFolder;

    /**
     * The File representation.
     */
    final private File file;

    /**
     * Constructor.
     * 
     * @param pName the name of the file.
     * @param pFolder the parent folder.
     * 
     * @throws IllegalArgumentException if one of the parameters is null.
     */
    public DocFile(String pName, DocFolder pFolder) {
        super();

        if (pName == null) {
            throw new IllegalArgumentException("The name cannot be null");
        }

        if (pFolder == null) {
            throw new IllegalArgumentException(
                    "The parent folder cannot be null");
        }

        name = pName;
        parentFolder = pFolder;

        file = new File(parentFolder.getFile(), pName);
    }

    /**
     * Returns the name of the file.
     * 
     * @return the name of the file.
     */
    public String getName() {

        return name;
    }

    /**
     * Return the parent DocFolder.
     * 
     * @return the DocFolder.
     */
    public DocFolder getFolder() {

        return parentFolder;
    }

    /**
     * Returns the File representation.
     * 
     * @return the File.
     */
    public File getFile() {

        return file;
    }

    /**
     * Returns a list with the folders from root.
     * 
     * @return a list with the folders from root.
     */
    public List<DocFolder> getPathFolders() {

        return parentFolder.getPathFolders();
    }

    /**
     * Return a path-like reference to this file starting on the specified
     * folder. The folder must be a parent folder.
     * 
     * @param folder the folder.
     * 
     * @return a path-like reference string.
     */
    private String buildRefFromFolder(DocFolder folder) {

        StringBuffer result = new StringBuffer();

        List<DocFolder> folders = getPathFolders();

        int index = folders.indexOf(folder);

        if (index == -1) {
            throw new IllegalArgumentException(
                    "The specified folder is not on this file's path: "
                            + folder);
        }

        for (index++; index < folders.size(); index++) {
            DocFolder f = (DocFolder) folders.get(index);
            result.append(f.getName() + '/');
        }

        result.append(getName() );

        return result.toString();
    }

    /**
     * Return a path-like reference to the specified file.
     * 
     * @param target the target file.
     * 
     * @return a path-like reference string.
     */
    public String buildRefTo(DocFile target) {

        List<DocFolder> tgtFileFolders = target.getPathFolders();

        StringBuffer ref = new StringBuffer();

        DocFolder localFolder = this.parentFolder;
        while (localFolder != null) {
            if (tgtFileFolders.contains(localFolder) ) {
                ref.append(target.buildRefFromFolder(localFolder) );
                String result = ref.toString();
                return result;
            } 
            else {
                ref.append("../");
                localFolder = localFolder.getParent();
            }
        }

        throw new IllegalArgumentException("No parent folder in common");
    }

    /**
     * Return a String representation of this file.
     * 
     * @return a String.
     */
    public String toString() {
        return name;
    }

}

