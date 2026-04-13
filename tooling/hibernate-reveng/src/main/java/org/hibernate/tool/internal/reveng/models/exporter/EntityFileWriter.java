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
package org.hibernate.tool.internal.reveng.models.exporter;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import org.hibernate.models.spi.ClassDetails;

/**
 * Utility for writing per-entity export output to files.
 * Handles package directory creation and file naming.
 *
 * @author Koen Aers
 */
public class EntityFileWriter {

	@FunctionalInterface
	public interface EntityWriteAction {
		void write(Writer writer, ClassDetails entity) throws Exception;
	}

	/**
	 * Resolves the output file for a given class name and extension.
	 */
	public static File resolveOutputFile(File outputDir, String className, String extension) {
		String simpleName;
		String packagePath;
		int lastDot = className.lastIndexOf('.');
		if (lastDot >= 0) {
			simpleName = className.substring(lastDot + 1);
			packagePath = className.substring(0, lastDot)
					.replace('.', File.separatorChar);
		} else {
			simpleName = className;
			packagePath = null;
		}
		File dir = (packagePath != null && !packagePath.isEmpty())
				? new File(outputDir, packagePath)
				: outputDir;
		return new File(dir, simpleName + extension);
	}

	/**
	 * For each entity, creates the appropriate file in the output
	 * directory (based on class name and extension) and invokes
	 * the write action.
	 *
	 * @param entities  the entities to export
	 * @param outputDir the root output directory
	 * @param extension the file extension (e.g. ".java", ".hbm.xml")
	 * @param action    the write action to invoke per entity
	 */
	public static void writePerEntity(
			List<ClassDetails> entities,
			File outputDir,
			String extension,
			EntityWriteAction action) {
		for (ClassDetails entity : entities) {
			String className = entity.getClassName();
			String simpleName;
			String packagePath;
			int lastDot = className.lastIndexOf('.');
			if (lastDot >= 0) {
				simpleName = className.substring(lastDot + 1);
				packagePath = className.substring(0, lastDot)
						.replace('.', File.separatorChar);
			}
			else {
				simpleName = className;
				packagePath = null;
			}
			File dir = (packagePath != null && !packagePath.isEmpty())
					? new File(outputDir, packagePath)
					: outputDir;
			dir.mkdirs();
			File outputFile = new File(dir, simpleName + extension);
			try (Writer writer = new FileWriter(outputFile)) {
				action.write(writer, entity);
			}
			catch (Exception e) {
				throw new RuntimeException(
						"Failed to export " + className
						+ " to " + outputFile, e);
			}
		}
	}
}
