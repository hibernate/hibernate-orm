/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.doc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.tool.internal.export.doc.DocFile;
import org.hibernate.tool.internal.export.doc.DocFolder;

/**
 * Manages the documentation file structure for entity documentation.
 * Creates the folder hierarchy and maps entities to their output files.
 * <p>
 * Templates access this object as {@code docFileManager} in the
 * FreeMarker context, calling methods like {@code getRef(from, to)},
 * {@code getCssStylesDocFile()}, {@code getEntityDocFile(entity)}, etc.
 *
 * @author Koen Aers
 */
public class EntityDocFileManager {

	private final DocFolder rootDocFolder;
	private final DocFile mainIndexDocFile;
	private final DocFile hibernateImageDocFile;
	private final DocFile extendsImageDocFile;
	private final DocFile cssStylesDocFile;
	private final DocFile classIndexDocFile;
	private final DocFile entitySummaryDocFile;
	private final DocFile allPackagesDocFile;
	private final DocFile allEntitiesDocFile;
	private final DocFile tableIndexDocFile;

	private final Map<String, DocFile> entityDocFiles = new HashMap<>();
	private final Map<String, DocFile> packageSummaryDocFiles = new HashMap<>();
	private final Map<String, DocFile> packageEntityListDocFiles = new HashMap<>();

	public EntityDocFileManager(EntityDocHelper docHelper, File rootFolder) {
		rootDocFolder = new DocFolder(rootFolder);
		mainIndexDocFile = new DocFile("index.html", rootDocFolder);

		DocFolder assetsFolder = new DocFolder("assets", rootDocFolder);
		hibernateImageDocFile = new DocFile("hibernate_logo.gif", assetsFolder);
		extendsImageDocFile = new DocFile("inherit.gif", assetsFolder);
		cssStylesDocFile = new DocFile("doc-style.css", assetsFolder);

		// Entity documentation folder
		DocFolder entitiesFolder = new DocFolder("entities", rootDocFolder);
		classIndexDocFile = new DocFile("index.html", entitiesFolder);
		entitySummaryDocFile = new DocFile("summary.html", entitiesFolder);
		allPackagesDocFile = new DocFile("allpackages.html", entitiesFolder);
		allEntitiesDocFile = new DocFile("allentities.html", entitiesFolder);

		// Tables folder (placeholder for header navigation)
		DocFolder tablesFolder = new DocFolder("tables", rootDocFolder);
		tableIndexDocFile = new DocFile("index.html", tablesFolder);

		// Create per-package folders and per-entity files
		for (String packageName : docHelper.getPackages()) {
			DocFolder packageFolder;
			DocFolder currentRoot = entitiesFolder;

			if (!packageName.equals(EntityDocHelper.DEFAULT_NO_PACKAGE)) {
				String[] parts = packageName.split("\\.");
				for (String part : parts) {
					packageFolder = new DocFolder(part, currentRoot);
					currentRoot = packageFolder;
				}

				DocFile summaryFile = new DocFile("summary.html", currentRoot);
				packageSummaryDocFiles.put(packageName, summaryFile);

				DocFile entityListFile =
						new DocFile("entities.html", currentRoot);
				packageEntityListDocFiles.put(packageName, entityListFile);
			}
			else {
				currentRoot = entitiesFolder;
			}

			for (EntityDocInfo entity :
					docHelper.getClasses(packageName)) {
				String fileName = entity.getDeclarationName() + ".html";
				DocFile entityFile = new DocFile(fileName, currentRoot);
				entityDocFiles.put(
						entity.getQualifiedDeclarationName(), entityFile);
			}
		}
	}

	public DocFolder getRootDocFolder() {
		return rootDocFolder;
	}

	public DocFile getMainIndexDocFile() {
		return mainIndexDocFile;
	}

	public DocFile getHibernateImageDocFile() {
		return hibernateImageDocFile;
	}

	public DocFile getExtendsImageDocFile() {
		return extendsImageDocFile;
	}

	public DocFile getCssStylesDocFile() {
		return cssStylesDocFile;
	}

	public DocFile getClassIndexDocFile() {
		return classIndexDocFile;
	}

	public DocFile getClassSummaryFile() {
		return entitySummaryDocFile;
	}

	public DocFile getAllPackagesDocFile() {
		return allPackagesDocFile;
	}

	public DocFile getAllEntitiesDocFile() {
		return allEntitiesDocFile;
	}

	public DocFile getTableIndexDocFile() {
		return tableIndexDocFile;
	}

	public DocFile getEntityDocFile(EntityDocInfo entity) {
		return entityDocFiles.get(entity.getQualifiedDeclarationName());
	}

	public DocFile getEntityDocFileByDeclarationName(EntityDocInfo entity) {
		DocFile file = getEntityDocFile(entity);
		if (file != null) {
			return file;
		}
		String targetName = entity.getQualifiedDeclarationName();
		for (Map.Entry<String, DocFile> entry : entityDocFiles.entrySet()) {
			if (entry.getKey().equals(targetName)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public DocFile getPackageSummaryDocFile(String packageName) {
		return packageSummaryDocFiles.get(packageName);
	}

	public DocFile getPackageEntityListDocFile(String packageName) {
		return packageEntityListDocFiles.get(packageName);
	}

	public String getRef(DocFile from, DocFile to) {
		if (from == null) {
			throw new IllegalArgumentException("From cannot be null.");
		}
		if (to == null) {
			throw new IllegalArgumentException("To cannot be null.");
		}
		return from.buildRefTo(to);
	}
}
