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

package org.hibernate.tool.hbm2x.Hbm2JavaEjb3Test;

import jakarta.persistence.Persistence;
import org.hibernate.Version;
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
import org.hibernate.tool.internal.util.AnnotationBuilder;
import org.hibernate.tool.internal.util.IteratorTransformer;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
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
	public void testEqualsHashCode() {
		PersistentClass classMapping = metadata.getEntityBinding("org.hibernate.tool.hbm2x.Hbm2JavaEjb3Test.Passenger");
		POJOClass clazz = new Cfg2JavaTool().getPOJOClass(classMapping);
		assertFalse(clazz.needsEqualsHashCode());
		classMapping = metadata.getEntityBinding("org.hibernate.tool.hbm2x.Hbm2JavaEjb3Test.Article");
		clazz = new Cfg2JavaTool().getPOJOClass(classMapping);
		assertTrue(clazz.needsEqualsHashCode());
	}
	
	@Test
	public void testAnnotationColumnDefaults() {
		PersistentClass classMapping = metadata.getEntityBinding("org.hibernate.tool.hbm2x.Hbm2JavaEjb3Test.Article");
		Cfg2JavaTool cfg2java = new Cfg2JavaTool();
		POJOClass clazz = cfg2java.getPOJOClass(classMapping);
		Property p = classMapping.getProperty("content");
		String string = clazz.generateAnnColumnAnnotation( p );
		assertNotNull(string);
		assertEquals(-1, string.indexOf("unique="));
		assertTrue(string.contains("nullable="));
		assertEquals(-1, string.indexOf("insertable="));
		assertEquals(-1, string.indexOf("updatable="));
		assertTrue(string.indexOf("length=10000")>0);
		p = classMapping.getProperty("name");
		string = clazz.generateAnnColumnAnnotation( p );
		assertNotNull(string);
		assertEquals(-1, string.indexOf("unique="));
		assertTrue(string.contains("nullable="));
		assertEquals(-1, string.indexOf("insertable="));
		assertTrue(string.indexOf("updatable=false")>0);
		assertTrue(string.indexOf("length=100")>0);
		classMapping = metadata.getEntityBinding( "org.hibernate.tool.hbm2x.Hbm2JavaEjb3Test.Train" );
		clazz = cfg2java.getPOJOClass(classMapping);
		p = classMapping.getProperty( "name" );
		string = clazz.generateAnnColumnAnnotation( p );
		assertNotNull(string);
		assertTrue(string.indexOf("unique=true")>0);
		assertTrue(string.contains("nullable="));
		assertEquals(-1, string.indexOf("insertable="));
		assertEquals(-1,string.indexOf("updatable="));
		assertEquals(-1, string.indexOf("length="));
	}
	
	@Test
	public void testEmptyCascade() {
		PersistentClass classMapping = metadata.getEntityBinding("org.hibernate.tool.hbm2x.Hbm2JavaEjb3Test.Article");
		Cfg2JavaTool cfg2java = new Cfg2JavaTool();
		EntityPOJOClass clazz = (EntityPOJOClass) cfg2java.getPOJOClass(classMapping);
		Property property = classMapping.getProperty( "author" );
		assertEquals(0, clazz.getCascadeTypes( property ).length);
		assertNull(
				FileUtil.findFirstString(
						"cascade={}", 
						new File(
								srcDir, 
								"org/hibernate/tool/hbm2x/Hbm2JavaEjb3Test/Article.java") ));
	}
		
	@Test
	public void testAnnotationBuilder() {
		AnnotationBuilder builder =  AnnotationBuilder.createAnnotation("SingleCleared").resetAnnotation( "Single" );
		assertEquals("@Single", builder.getResult());
		builder = AnnotationBuilder.createAnnotation("jakarta.persistence.OneToMany")
				    .addAttribute("willbecleared", (String)null)
				    .resetAnnotation("jakarta.persistence.OneToMany")
					.addAttribute("cascade", new String[] { "val1", "val2"})
					.addAttribute("fetch", "singleValue");
		assertEquals("@jakarta.persistence.OneToMany(cascade={val1, val2}, fetch=singleValue)", builder.getResult());
		builder = AnnotationBuilder.createAnnotation("jakarta.persistence.OneToMany");
		builder.addAttribute("cascade", (String[])null);
		builder.addAttribute("fetch", (String)null);
		assertEquals("@jakarta.persistence.OneToMany", builder.getResult());
		builder = AnnotationBuilder.createAnnotation("abc");
		ArrayList<Object> list = new ArrayList<>();
		list.add(42);
		list.add("xxx");
		builder.addQuotedAttributes( "it", list.iterator() );
		assertEquals("@abc(it={\"42\", \"xxx\"})", builder.getResult());		
		List<String> columns = new ArrayList<>();
		columns.add("first");
		columns.add("second");
		AnnotationBuilder constraint = AnnotationBuilder.createAnnotation( "UniqueConstraint" );
		constraint.addQuotedAttributes( "columnNames", new IteratorTransformer<>(columns.iterator()) {
            public String transform(String object) {
                return object;
            }
        });
		constraint.addAttribute( "single", "value" );	
		String attribute = constraint.getAttributeAsString("columnNames");
		assertEquals("{\"first\", \"second\"}", attribute);
		assertEquals("value", constraint.getAttributeAsString( "single" ));
	}
	
}
