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

package org.hibernate.tool.hbm2x.Hbm2JavaTest;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.export.DefaultArtifactCollector;
import org.hibernate.tool.internal.exporter.entity.ImportContext;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Customer.hbm.xml",
			"Order.hbm.xml",
			"LineItem.hbm.xml",
			"Product.hbm.xml",
			"HelloWorld.hbm.xml",
			"Train.hbm.xml",
			"Passenger.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private MetadataDescriptor metadataDescriptor = null;
	private File srcDir = null;
    private DefaultArtifactCollector artifactCollector = null;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		artifactCollector = new DefaultArtifactCollector();
		exporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		exporter.start();
	}

	@Test
	public void testFileExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Customer.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/LineItem.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Train.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Passenger.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Product.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"generated/BaseHelloWorld.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"HelloUniverse.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/FatherComponent.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/ChildComponent.java"));
		assertEquals(15, artifactCollector.getFileCount("java"));
	}

	@Test
	public void testCompilable() throws Exception {
		String helloWorldResourcePath = "/org/hibernate/tool/hbm2x/Hbm2JavaTest/HelloWorld.java_";
		File helloWorldOrigin = new File(Objects.requireNonNull(getClass().getResource(helloWorldResourcePath)).toURI());
		File helloWorldDestination = new File(srcDir, "HelloWorld.java");
		File targetDir = new File(outputFolder, "compilerOutput" );
		assertTrue(targetDir.mkdir());
		Files.copy(helloWorldOrigin.toPath(), helloWorldDestination.toPath());
		JavaUtil.compile(srcDir, targetDir);
		assertTrue(new File(targetDir, "HelloWorld.class").exists());
	}

	@Test
	public void testNoFreeMarkerLeftOvers() {
		assertNull(FileUtil.findFirstString(
				"$",
				new File(
						srcDir,
						"org/hibernate/tool/hbm2x/Hbm2JavaTest/Customer.java")));
		assertNull(FileUtil.findFirstString(
				"$",
				new File(
						srcDir,
						"org/hibernate/tool/hbm2x/Hbm2JavaTest/LineItem.java")));
		assertNull(FileUtil.findFirstString(
				"$",
				new File(
						srcDir,
						"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java")));
		assertNull(FileUtil.findFirstString(
				"$",
				new File(
						srcDir,
						"org/hibernate/tool/hbm2x/Hbm2JavaTest/Product.java")));
		assertNull(FileUtil.findFirstString(
				"$",
				new File(
						srcDir,
						"org/hibernate/tool/hbm2x/Hbm2JavaTest/Address.java")));
	}

	@Test
	public void testPackageName() throws Exception {
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		assertTrue(orderSource.contains("package org.hibernate.tool.hbm2x.Hbm2JavaTest;"),
				"Order should have correct package declaration");
		// HelloWorld has generated-class=generated.BaseHelloWorld, so package is "generated"
		String hwSource = Files.readString(new File(srcDir,
				"generated/BaseHelloWorld.java").toPath());
		assertTrue(hwSource.contains("package generated;"),
				"BaseHelloWorld should honor generated-class package");
	}

	@Test
	public void testFieldNotThere() {
		assertNull(FileUtil.findFirstString(
				"notgenerated",
				new File(
						srcDir,
						"HelloUniverse.java")));
	}

	@Test
	public void testJavaDoc() throws Exception {
		// HelloWorld has class-description="Hey there"
		String hwSource = Files.readString(new File(srcDir,
				"generated/BaseHelloWorld.java").toPath());
		assertTrue(hwSource.contains("Hey there"),
				"BaseHelloWorld should contain class javadoc from class-description meta");
	}

	@Test
	public void testExtraCode() throws Exception {
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		assertTrue(orderSource.contains("// extra code line 1"),
				"Order should contain extra code line 1");
		assertTrue(orderSource.contains("// extra code line 2"),
				"Order should contain extra code line 2");
		assertTrue(orderSource.contains("Collator.getInstance()"),
				"Order should contain Collator extra code");
	}

	@Test
	public void testScope() throws Exception {
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		assertTrue(orderSource.contains("public class Order"),
				"Order should be a public class");
		String hwSource = Files.readString(new File(srcDir,
				"generated/BaseHelloWorld.java").toPath());
		assertTrue(hwSource.contains("public interface BaseHelloWorld"),
				"BaseHelloWorld should be a public interface");
	}

	@Test
	public void testDeclarationType() throws Exception {
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		assertTrue(orderSource.contains("class Order"),
				"Order should be declared as class");
		String hwSource = Files.readString(new File(srcDir,
				"generated/BaseHelloWorld.java").toPath());
		assertTrue(hwSource.contains("interface BaseHelloWorld"),
				"BaseHelloWorld should be declared as interface");
	}

	@Test
	public void testTypeName() throws Exception {
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		assertTrue(orderSource.contains("Collection") && orderSource.contains("lineItems"),
				"Order.lineItems should be declared as a Collection");
	}

	@Test
	public void testUseRawTypeNullability() throws Exception {
		String productSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Product.java").toPath());
		// numberAvailable: not-null, type="int" → should be primitive int
		assertTrue(productSource.contains("int numberAvailable") ||
						productSource.contains("int getNumberAvailable"),
				"numberAvailable (not-null int) should use primitive type");
		// minStock: nullable, type="long" → should be primitive long (type attribute overrides)
		assertTrue(productSource.contains("long minStock") ||
						productSource.contains("long getMinStock"),
				"minStock (long type) should use long");
		// otherStock: not-null, but property-type meta overrides to Integer
		assertTrue(productSource.contains("Integer otherStock") ||
						productSource.contains("Integer getOtherStock"),
				"otherStock should use Integer (overridden by property-type meta)");
	}

	@Test
	public void testExtendsImplements() throws Exception {
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		// Order has no extends meta attribute
		assertFalse(orderSource.matches("(?s).*class Order extends \\w+.*"),
				"Order should not extend anything (other than implicit Object)");
		// HelloWorld/BaseHelloWorld: meta extends=Comparable, interface=true
		String hwSource = Files.readString(new File(srcDir,
				"generated/BaseHelloWorld.java").toPath());
		assertTrue(hwSource.contains("extends Comparable"),
				"BaseHelloWorld interface should extend Comparable");
		// Interface should not have implements
		assertFalse(hwSource.contains("implements"),
				"Interface should not have implements declaration");
	}

	@Test
	public void testDeclarationName() throws Exception {
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		assertTrue(orderSource.contains("class Order"),
				"Order declaration name should be Order");
		String hwSource = Files.readString(new File(srcDir,
				"generated/BaseHelloWorld.java").toPath());
		assertTrue(hwSource.contains("BaseHelloWorld"),
				"HelloWorld declaration name should be BaseHelloWorld (from generated-class meta)");
	}

	@Test
	public void testPropertiesForFullConstructor() throws Exception {
		// HelloWorld has meta interface=true and generated-class=BaseHelloWorld,
		// so both BaseHelloWorld and HelloUniverse are generated as interfaces.
		// Verify the generated interfaces declare the correct accessor methods.
		File hwFile = new File(srcDir, "generated/BaseHelloWorld.java");
		String hwSource = Files.readString(hwFile.toPath());
		// BaseHelloWorld: 3 own properties (id, hello, world)
		assertTrue(hwSource.contains("public interface BaseHelloWorld"),
				"BaseHelloWorld should be generated as an interface");
		assertTrue(hwSource.contains("public String getId()"));
		assertTrue(hwSource.contains("public String getHello()"));
		assertTrue(hwSource.contains("public long getWorld()"));
		// HelloUniverse extends HelloWorld: inherits 3 + dimension, address
		// (notgenerated excluded by gen-property=false)
		File huFile = new File(srcDir, "HelloUniverse.java");
		String huSource = Files.readString(huFile.toPath());
		assertTrue(huSource.contains("public interface HelloUniverse extends HelloWorld"),
				"HelloUniverse should extend HelloWorld");
		assertTrue(huSource.contains("public String getDimension()"));
		assertTrue(huSource.contains("public UniversalAddress getAddress()"));
		assertFalse(huSource.contains("getNotgenerated"),
				"notgenerated (gen-property=false) should not appear in generated code");
	}

	@Test
	public void testToString() throws Exception {
		// BaseHelloWorld is an interface, so it has no toString
		String hwSource = Files.readString(new File(srcDir,
				"generated/BaseHelloWorld.java").toPath());
		assertFalse(hwSource.contains("toString()"),
				"BaseHelloWorld is an interface, should not have toString");
		// Order is a class, check that toString is generated
		String orderSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Order.java").toPath());
		assertTrue(orderSource.contains("toString()"),
				"Order should have toString");
	}

	@Test
	public void testImportOfSameName() {
		ImportContext ic = new ImportContext("foobar");
		assertEquals("CascadeType", ic.importType("jakarta.persistence.CascadeType"));
		assertEquals("org.hibernate.annotations.CascadeType", ic.importType("org.hibernate.annotations.CascadeType"));
        assertFalse(ic.generateImports().contains("hibernate"), "The hibernate annotation should not be imported to avoid name clashes");
	}

	@Test
	public void testImporter() {
		ImportContext context = new ImportContext( "org.hibernate" );
		assertEquals("byte", context.importType("byte"));
		assertEquals("Session", context.importType("org.hibernate.Session"));
		assertEquals("Long", context.importType("java.lang.Long"));
		assertEquals("org.test.Session", context.importType("org.test.Session"));
		assertEquals("Entity", context.importType("org.test.Entity"));
		assertEquals("org.other.test.Entity", context.importType("org.other.test.Entity"));
		assertEquals("Collection<org.marvel.Hulk>", context.importType("java.util.Collection<org.marvel.Hulk>"));
		assertEquals("Map<java.lang.String, org.marvel.Hulk>", context.importType("java.util.Map<java.lang.String, org.marvel.Hulk>"));
		assertEquals("Collection<org.marvel.Hulk>[]", context.importType("java.util.Collection<org.marvel.Hulk>[]"));
		assertEquals("Map<java.lang.String, org.marvel.Hulk>", context.importType("java.util.Map<java.lang.String, org.marvel.Hulk>"));
		String string = context.generateImports();
        assertFalse(string.contains("import org.hibernate.Session;"));
		assertTrue(string.indexOf("import org.test.Entity;")>0);
        assertFalse(string.contains("import org.other.test.Entity;"), "Entity can only be imported once");
		assertFalse(string.contains("<"));
		assertEquals("Outer.Entity", context.importType("org.test.Outer$Entity"));
		assertEquals("org.other.test.Outer.Entity", context.importType("org.other.test.Outer$Entity"));
		assertEquals("Collection<org.marvel.Outer.Hulk>", context.importType("java.util.Collection<org.marvel.Outer$Hulk>"));
		assertEquals("Map<java.lang.String, org.marvel.Outer.Hulk>", context.importType("java.util.Map<java.lang.String, org.marvel.Outer$Hulk>"));
		assertEquals("Collection<org.marvel.Outer.Hulk>[]", context.importType("java.util.Collection<org.marvel.Outer$Hulk>[]"));
		assertEquals("Map<java.lang.String, org.marvel.Outer.Hulk>", context.importType("java.util.Map<java.lang.String, org.marvel.Outer$Hulk>"));
		//assertEquals("Test.Entry", context.importType("org.hibernate.Test.Entry")); what should be the behavior for this ?
		assertEquals("Test.Entry", context.importType("org.hibernate.Test$Entry"));
		assertEquals("Map.Entry", context.importType("java.util.Map$Entry"));
		assertEquals("Entry", context.importType("java.util.Map.Entry")); // we can't detect that it is the same class here unless we try and load all strings so we fall back to default class name.
		assertEquals("List<java.util.Map.Entry>", context.importType( "java.util.List<java.util.Map$Entry>" ));
		assertEquals("List<org.hibernate.Test.Entry>", context.importType( "java.util.List<org.hibernate.Test$Entry>" ));
		string = context.generateImports();
		assertTrue(string.contains("import java.util.Map"));
        assertFalse(string.contains("import java.utilMap$"));
        assertFalse(string.contains("$"));
	}

	@Test
	public void testEqualsHashCode() throws Exception {
		// Customer's addressComponent has use-in-equals on streetAddress1, city, verified
		String addressSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Address.java").toPath());
		assertTrue(addressSource.contains("equals("),
				"Address component should have equals (has use-in-equals properties)");
		assertTrue(addressSource.contains("hashCode("),
				"Address component should have hashCode (has use-in-equals properties)");
	}

	@Test
	public void testGenerics() throws Exception {
		File genericsSource = new File(outputFolder, "genericssource");
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, genericsSource);
		artifactCollector = new DefaultArtifactCollector();
		exporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		exporter.getProperties().setProperty("jdk5", "true");
		exporter.start();
		File genericsTarget = new File(outputFolder, "genericstarget" );
		assertTrue(genericsTarget.mkdir());
		String helloWorldResourcePath = "/org/hibernate/tool/hbm2x/Hbm2JavaTest/HelloWorld.java_";
		File helloWorldOrigin = new File(Objects.requireNonNull(getClass().getResource(helloWorldResourcePath)).toURI());
		File helloWorldDestination = new File(genericsSource, "HelloWorld.java");
		Files.copy(helloWorldOrigin.toPath(), helloWorldDestination.toPath());
		JavaUtil.compile(genericsSource, genericsTarget);
		assertTrue(new File(genericsTarget, "HelloWorld.class").exists());
	}

	@Test
	public void testDynamicComponent() {
		// Build a ClassDetails from an HBM with a <dynamic-component> and verify
		// that the field type is java.util.Map
		var ctx = new org.hibernate.tool.internal.builder.hbm.HbmBuildContext();
		var modelsContext = ctx.getModelsContext();
		var entityClass = new org.hibernate.models.internal.dynamic.DynamicClassDetails(
				"DynTest", "org.test.DynTest", Object.class,
				false, null, null, modelsContext);
		var dynComponent = new org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType();
		dynComponent.setName("dynaMap");
		org.hibernate.tool.internal.builder.hbm.HbmComponentBuilder
				.processDynamicComponent(entityClass, dynComponent, "org.test", ctx);
		var field = entityClass.findFieldByName("dynaMap");
		assertNotNull(field);
		assertEquals("java.util.Map", field.getType().determineRawClass().getClassName());
	}

	@Test
	public void testUserTypes() throws Exception {
		// Customer.customDate uses DummyDateType (maps to java.sql.Date)
		String customerSource = Files.readString(new File(srcDir,
				"org/hibernate/tool/hbm2x/Hbm2JavaTest/Customer.java").toPath());
		assertTrue(customerSource.contains("Date") && customerSource.contains("customDate"),
				"customDate should be typed based on user type mapping");
	}
}
