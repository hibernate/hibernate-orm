/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.export.doc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a documentation folder.
 * 
 * @author Ricardo C. Moral
 */
public class DocFolder {

    /**
     * The name of the folder.
     */
    private String name;

    /**
     * The parent folder.
     */
    private DocFolder parent;

    /**
     * The File instance.
     */
    private final File file;

    /**
     * Holds a list with the folders that are between this folder and root.
     */
    private final List<DocFolder> pathFolders = new ArrayList<>();

    /**
     * Constructor for the root folder.
     */
    public DocFolder(File root) {
        super();

        if (root == null) {
            throw new IllegalArgumentException("Root File cannot be null");
        }

        file = root;
        
        pathFolders.add(this);
    }

    /**
     * Constructor.
     * 
     * @param pName the name of the file.
     * @param pParent the parent folder.
     */
    public DocFolder(String pName, DocFolder pParent) {
        super();

        if (pName == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        if (pParent == null) {
            throw new IllegalArgumentException("Parent folder cannot be null");
        }

        name = pName;
        parent = pParent;

        file = new File(parent.getFile(), name);

        if (file.exists() ) {
            if (!file.isDirectory() ) {
                throw new RuntimeException("The path: "
                        + file.getAbsolutePath()
                        + " exists, but is not a Folder");
            }
        } 
        else {
            if (!file.mkdirs() ) {
                throw new RuntimeException("unable to create folder: "
                        + file.getAbsolutePath() );
            }
        }
        pathFolders.addAll(parent.getPathFolders() );
        pathFolders.add(this);
    }

    /**
     * Return the name of this folder.
     * 
     * @return the name of this folder.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the parent folder.
     * 
     * @return the parent folder.
     */
    public DocFolder getParent() {
        return parent;
    }

    /**
     * Return the File instance.
     * 
     * @return the File instance.
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
        return pathFolders;
    }

    /**
     * Return a String representation of this folder.
     * 
     * @return a String.
     */
    public String toString() {
        return name;
    }

}

