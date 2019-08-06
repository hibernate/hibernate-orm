package org.hibernate.tool.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.types.FileSet;

public class MetadataTask {
	
	File propertyFile = null;
	File configFile = null;
	List<FileSet> fileSets = new ArrayList<FileSet>();	
	
	public void setPropertyFile(File file) {
		this.propertyFile = file;
	}
	
	public void setConfigFile(File file) {
		this.configFile = file;
	}
	
	public void addConfiguredFileSet(FileSet fileSet) {
		fileSets.add(fileSet);
	}
	
}
