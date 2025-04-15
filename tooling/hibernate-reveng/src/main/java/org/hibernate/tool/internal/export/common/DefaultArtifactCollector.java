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
package org.hibernate.tool.internal.export.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.tool.api.export.ArtifactCollector;
import org.hibernate.tool.api.xml.XMLPrettyPrinter;

/**
 * Callback class that all exporters are given to allow better feedback and
 * processing of the output afterwards.
 * 
 * @author Max Rydahl Andersen
 *
 */
public class DefaultArtifactCollector implements ArtifactCollector {

	final protected Map<String, List<File>> files = new HashMap<String, List<File>>();

	/* (non-Javadoc)
	 * @see org.hibernate.tool.internal.export.ArtifactCollector#addFile(java.io.File, java.lang.String)
	 */
	@Override
	public void addFile(File file, String type) {
		List<File> existing = files.get(type);
		if (existing == null) {
			existing = new ArrayList<File>();
			files.put(type, existing);
		}
		existing.add(file);
	}

	/* (non-Javadoc)
	 * @see org.hibernate.tool.internal.export.ArtifactCollector#getFileCount(java.lang.String)
	 */
	@Override
	public int getFileCount(String type) {
		List<File> existing = files.get(type);

		return (existing == null) ? 0 : existing.size();
	}

	/* (non-Javadoc)
	 * @see org.hibernate.tool.internal.export.ArtifactCollector#getFiles(java.lang.String)
	 */
	@Override
	public File[] getFiles(String type) {
		List<File> existing = files.get(type);

		if (existing == null) {
			return new File[0];
		} else {
			return (File[]) existing.toArray(new File[existing.size()]);
		}
	}

	/* (non-Javadoc)
	 * @see org.hibernate.tool.internal.export.ArtifactCollector#getFileTypes()
	 */
	@Override
	public Set<String> getFileTypes() {
		return files.keySet();
	}

	/* (non-Javadoc)
	 * @see org.hibernate.tool.internal.export.ArtifactCollector#formatFiles()
	 */
	@Override
	public void formatFiles() {

		formatXml("xml");
		formatXml("hbm.xml");
		formatXml("cfg.xml");

	}

	private void formatXml(String type) {
		List<File> list = files.get(type);
		if (list != null && !list.isEmpty()) {
			for (Iterator<File> iter = list.iterator(); iter.hasNext();) {
				File xmlFile = iter.next();
				try {
					XMLPrettyPrinter.prettyPrintFile(xmlFile);
				} catch (IOException e) {
					throw new RuntimeException("Could not format XML file: " + xmlFile, e);
				}
			}
		}
	}

}
