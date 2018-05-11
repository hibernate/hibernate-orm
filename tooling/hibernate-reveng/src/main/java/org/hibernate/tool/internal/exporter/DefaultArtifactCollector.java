package org.hibernate.tool.internal.exporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.hbm2x.ExporterException;
import org.hibernate.tool.hbm2x.XMLPrettyPrinter;

/**
 * Callback class that all exporters are given to allow better feedback and
 * processing of the output afterwards.
 * 
 * @author Max Rydahl Andersen
 *
 */
public class DefaultArtifactCollector {

	final protected Map<String, List<File>> files = new HashMap<String, List<File>>();

	/**
	 * Called to inform that a file has been created by the exporter.
	 */
	public void addFile(File file, String type) {
		List<File> existing = files.get(type);
		if (existing == null) {
			existing = new ArrayList<File>();
			files.put(type, existing);
		}
		existing.add(file);
	}

	public int getFileCount(String type) {
		List<File> existing = files.get(type);

		return (existing == null) ? 0 : existing.size();
	}

	public File[] getFiles(String type) {
		List<File> existing = files.get(type);

		if (existing == null) {
			return new File[0];
		} else {
			return (File[]) existing.toArray(new File[existing.size()]);
		}
	}

	public Set<String> getFileTypes() {
		return files.keySet();
	}

	public void formatFiles() {

		formatXml("xml");
		formatXml("hbm.xml");
		formatXml("cfg.xml");

	}

	private void formatXml(String type) throws ExporterException {
		List<File> list = files.get(type);
		if (list != null && !list.isEmpty()) {
			for (Iterator<File> iter = list.iterator(); iter.hasNext();) {
				File xmlFile = iter.next();
				try {
					XMLPrettyPrinter.prettyPrintFile(xmlFile);
				} catch (IOException e) {
					throw new ExporterException("Could not format XML file: " + xmlFile, e);
				}
			}
		}
	}

}
