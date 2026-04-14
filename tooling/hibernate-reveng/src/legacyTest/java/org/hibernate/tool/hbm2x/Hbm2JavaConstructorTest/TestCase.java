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

package org.hibernate.tool.hbm2x.Hbm2JavaConstructorTest;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Constructors.hbm.xml"
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
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		exporter.start();
	}

	@Test
	public void testCompilable() throws Exception {
		String constructorUsageResourcePath = "/org/hibernate/tool/hbm2x/Hbm2JavaConstructorTest/ConstructorUsage.java_";
		File constructorUsageOrigin = new File(Objects.requireNonNull(getClass().getResource(constructorUsageResourcePath)).toURI());
		File constructorUsageDestination = new File(srcDir, "ConstructorUsage.java");
		File targetDir = new File(outputFolder, "compilerOutput" );
		assertTrue(targetDir.mkdir());
		Files.copy(constructorUsageOrigin.toPath(), constructorUsageDestination.toPath());
		JavaUtil.compile(srcDir, targetDir);
		assertTrue(new File(targetDir, "ConstructorUsage.class").exists());
		assertTrue(new File(targetDir, "Company.class").exists());
		assertTrue(new File(targetDir, "BigCompany.class").exists());
		assertTrue(new File(targetDir, "EntityAddress.class").exists());
	}

	@Test
	public void testNoVelocityLeftOvers() {
		assertNull(FileUtil.findFirstString(
				"$",
				new File(srcDir, "Company.java" ) ) );
		assertNull(FileUtil.findFirstString(
				"$",
				new File(srcDir,"BigCompany.java" ) ) );
		assertNull(FileUtil.findFirstString(
				"$",
				new File(srcDir,"EntityAddress.java" ) ) );
	}

	@Test
	public void testEntityConstructorLogic() throws IOException {
		// Company has composite-id (CompanyId), brand (not-null), value (formula — excluded),
		// employees (set) — full constructor should have 3 params: id, brand, employees
		String companySource = readFile("Company.java");
		assertContains(companySource, "public Company(CompanyId id, String brand, Set employees)");
		// Minimal constructor: id + brand (not-null) only
		assertContains(companySource, "public Company(CompanyId id, String brand)");
		// Default constructor
		assertContains(companySource, "public Company()");

		// BigCompany extends Company, adds ceo (many-to-one)
		// Full constructor: superclass params + own = id, brand, employees, ceo
		String bigCompanySource = readFile("BigCompany.java");
		assertContains(bigCompanySource, "public BigCompany(CompanyId id, String brand, Set<Employee> employees, Employee ceo)");

		// Person has generated id, name (not-null), address (component)
		// Minimal constructor: name + address (not id, since generated)
		String personSource = readFile("Person.java");
		assertContains(personSource, "public Person(String name, EntityAddress address)");
		// Full constructor same as minimal (all non-id properties are not-null or component)
		assertContains(personSource, "public Person()");
	}

	@Test
	public void testMinimal() throws IOException {
		// BrandProduct has assigned id, version (excluded from constructors), name
		// Minimal: just id (assigned, so included)
		// Full: id + name (version excluded)
		String bpSource = readFile("BrandProduct.java");
		assertContains(bpSource, "public BrandProduct(String id)");
		assertContains(bpSource, "public BrandProduct(String id, String name)");
		assertContains(bpSource, "public BrandProduct()");
	}

	private String readFile(String fileName) throws IOException {
		return Files.readString(new File(srcDir, fileName).toPath());
	}

	private void assertContains(String source, String expected) {
		assertTrue(source.contains(expected),
				"Expected to find '" + expected + "' in generated source");
	}

}
