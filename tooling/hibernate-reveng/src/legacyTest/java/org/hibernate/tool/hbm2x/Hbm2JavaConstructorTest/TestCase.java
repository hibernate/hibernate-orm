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

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.java.Cfg2JavaTool;
import org.hibernate.tool.internal.export.java.EntityPOJOClass;
import org.hibernate.tool.internal.export.java.POJOClass;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Constructors.hbm.xml"
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File srcDir = null;

    private Metadata metadata = null;
	
	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		metadata = metadataDescriptor.createMetadata();
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
	public void testEntityConstructorLogic() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		POJOClass company = c2j.getPOJOClass(metadata.getEntityBinding("Company"));	
		List<Property> all = company.getPropertyClosureForFullConstructor();
		assertNoDuplicates(all);
		assertEquals(3, all.size());
		List<Property> superCons = company.getPropertyClosureForSuperclassFullConstructor();
		assertEquals(0, superCons.size(), "company is a base class, should not have superclass cons");
		List<Property> subCons = company.getPropertiesForFullConstructor();
		assertNoDuplicates(subCons);
		assertEquals(3, subCons.size());
		assertNoOverlap(superCons, subCons);
		POJOClass bigCompany = c2j.getPOJOClass(metadata.getEntityBinding("BigCompany"));
		List<Property> bigsuperCons = bigCompany.getPropertyClosureForSuperclassFullConstructor();
		assertNoDuplicates(bigsuperCons);
		//assertEquals(3, bigsuperCons.size());
		List<Property> bigsubCons = bigCompany.getPropertiesForFullConstructor();
		assertEquals(1, bigsubCons.size());
		assertNoOverlap(bigsuperCons, bigsubCons);
		List<?> bigall = bigCompany.getPropertyClosureForFullConstructor();
		assertNoDuplicates(bigall);
		assertEquals(4, bigall.size());
		PersistentClass classMapping = metadata.getEntityBinding("Person");
		POJOClass person = c2j.getPOJOClass(classMapping);
		List<Property> propertiesForMinimalConstructor = person.getPropertiesForMinimalConstructor();
		assertEquals(2,propertiesForMinimalConstructor.size());
		assertFalse(propertiesForMinimalConstructor.contains(classMapping.getIdentifierProperty()));
		List<Property> propertiesForFullConstructor = person.getPropertiesForFullConstructor();
		assertEquals(2,propertiesForFullConstructor.size());
		assertFalse(propertiesForFullConstructor.contains(classMapping.getIdentifierProperty()));	
	}

	@Test
	public void testMinimal() {
		POJOClass bp = new EntityPOJOClass(
				metadata.getEntityBinding("BrandProduct"), 
				new Cfg2JavaTool());
		List<Property> propertiesForMinimalConstructor = bp.getPropertiesForMinimalConstructor();
		assertEquals(1,propertiesForMinimalConstructor.size());
		List<Property> propertiesForFullConstructor = bp.getPropertiesForFullConstructor();
		assertEquals(2, propertiesForFullConstructor.size());		
	}
	
	private void assertNoDuplicates(List<?> bigall) {
        Set<Object> set = new HashSet<>(bigall);
		assertEquals(set.size(),bigall.size(), "list had duplicates!");	
	}

	private void assertNoOverlap(List<?> first, List<?> second) {
		Set<Object> set = new HashSet<>();
		set.addAll(first);
		set.addAll(second);	
		assertEquals(set.size(),first.size()+second.size());
	}

}
