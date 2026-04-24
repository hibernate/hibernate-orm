/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
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

package org.hibernate.tool.reveng.hbm2x.Hbm2JavaEjb3Test;

import jakarta.persistence.Persistence;
import org.hibernate.Version;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.test.utils.FileUtil;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Author.hbm.xml",
			"Article.hbm.xml",
			"Train.hbm.xml",
			"Passenger.hbm.xml"
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File srcDir = null;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		FileUtil.generateNoopComparator(srcDir);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		exporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[0]);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.getProperties().setProperty("jdk5", "true");
		exporter.start();
	}

	@Test
	public void testFileExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Author.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Article.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Train.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Passenger.java") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/TransportationPk.java") );
	}

	@Test
	public void testBasicComponent() {
		assertEquals( 
				"@Embeddable", 
				FileUtil.findFirstString( 
						"@Embeddable", 
						new File(
								srcDir,
								"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/TransportationPk.java")));
	}

	@Test
	public void testCompile() {
		File compiled = new File(outputFolder, "compiled");
		assertTrue(compiled.mkdir());
		List<String> jars = new ArrayList<>();
		jars.add(JavaUtil.resolvePathToJarFileFor(Persistence.class)); // for jpa api
		jars.add(JavaUtil.resolvePathToJarFileFor(Version.class)); // for hibernate core
		JavaUtil.compile(srcDir, compiled, jars);
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, 
				"comparator/NoopComparator.class") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Article.class") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Author.class") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Passenger.class") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Train.class") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, 
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/TransportationPk.class") );
	}

	@Test
	public void testEqualsHashCode() throws Exception {
		// Article has natural-id, so equals/hashCode should be based on natural-id
		String articleSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Article.java").toPath());
		assertTrue(articleSource.contains("equals("),
				"Article should have equals() (has natural-id)");
		assertTrue(articleSource.contains("hashCode("),
				"Article should have hashCode() (has natural-id)");
	}

	@Test
	public void testAnnotationColumnDefaults() throws Exception {
		String articleSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Article.java").toPath());
		// content: not-null, length=10000
		assertTrue(articleSource.contains("length = 10000") || articleSource.contains("length=10000"),
				"Article content should have length=10000");
		assertTrue(articleSource.contains("nullable = false") || articleSource.contains("nullable=false"),
				"Article content should have nullable=false");
		// name (natural-id property): not-null, length=100
		assertTrue(articleSource.contains("length = 100") || articleSource.contains("length=100"),
				"Article name should have length=100");
		// Train name: unique=true, not-null
		String trainSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Train.java").toPath());
		assertTrue(trainSource.contains("unique = true") || trainSource.contains("unique=true"),
				"Train name should have unique=true");
	}

	@Test
	public void testEmptyCascade() {
		// cascade="none" on many-to-one should not produce cascade={}
		assertNull(
				FileUtil.findFirstString(
						"cascade={}",
						new File(
								srcDir,
								"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Article.java") ));
	}
		
}
