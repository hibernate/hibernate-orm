package org.hibernate.tool.api.export;

import java.io.File;
import java.util.Set;

public interface ArtifactCollector {

	/**
	 * Called to inform that a file has been created by the exporter.
	 */
	void addFile(File file, String type);

	int getFileCount(String type);

	File[] getFiles(String type);

	Set<String> getFileTypes();

	void formatFiles();

}