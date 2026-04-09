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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.*;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.common.DefaultArtifactCollector;
import org.hibernate.tool.internal.export.java.*;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	private Metadata metadata = null;
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
		metadata = metadataDescriptor.createMetadata();
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
	
	// TODO Re-enable this test: HBX-1248
	@Disabled
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
	public void testPackageName() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass classMapping = metadata.getEntityBinding("org.hibernate.tool.hbm2x.Hbm2JavaTest.Order");
		POJOClass pc = c2j.getPOJOClass(classMapping);
		assertEquals( "org.hibernate.tool.hbm2x.Hbm2JavaTest", pc.getPackageName() );
		assertEquals( "package org.hibernate.tool.hbm2x.Hbm2JavaTest;", pc.getPackageDeclaration() );
		assertEquals( 
				"package generated;", 
				c2j.getPOJOClass(metadata.getEntityBinding("HelloWorld"))
					.getPackageDeclaration(),
				"did not honor generated-class");
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
	public void testJavaDoc() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		assertEquals( " * test", c2j.toJavaDoc( "test", 0 ) );
		assertEquals( "   * test", c2j.toJavaDoc( "test", 2 ) );
		assertEquals( "   * test\n   * me", c2j.toJavaDoc( "test\nme", 2 ) );
		PersistentClass local = metadata.getEntityBinding( "HelloWorld" );
		POJOClass pc = c2j.getPOJOClass(local);
		assertEquals( " * Hey there", pc.getClassJavaDoc( "fallback", 0 ) );
		assertEquals( 
				" * Test Field Description", 
				pc.getFieldJavaDoc(local.getIdentifierProperty(), 0 ) );
	}

	@Test
	public void testExtraCode() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		assertFalse(c2j.hasMetaAttribute(
				metadata.getEntityBinding("HelloWorld" ), "class-code" ) );
		PersistentClass classMapping = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
		assertEquals(
				"// extra code line 1\n// extra code line 2\n{ Collator.getInstance(); }",
				c2j.getPOJOClass(classMapping).getExtraClassCode() );
	}

	@Test
	public void testScope() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
		assertEquals( "public strictfp", c2j.getClassModifiers( pc ) );
		assertEquals("public", c2j.getClassModifiers(metadata.getEntityBinding( "HelloWorld" ) ) );
	}

	@Test
	public void testDeclarationType() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
		assertEquals( "class", c2j.getPOJOClass(pc).getDeclarationType() );
		assertEquals( "interface", c2j.getPOJOClass(metadata.getEntityBinding( "HelloWorld" ) ).getDeclarationType() );
	}

	@Test
	public void testTypeName() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
		Property property = pc.getProperty( "lineItems" );
		assertEquals( "java.util.Collection", c2j.getJavaTypeName( property, false ) );
	}

	@Test
	public void testUseRawTypeNullability() {
		Cfg2JavaTool c2j = new Cfg2JavaTool( /*true*/ );
		PersistentClass pc = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Product" );
		Property property = pc.getProperty( "numberAvailable" );
		assertFalse( property.getValue().isNullable() );
		assertEquals( 
				"int", 
				c2j.getJavaTypeName( property, false ),
				"typename should be used when rawtypemode");
		property = pc.getProperty( "minStock" );
		assertTrue( property.getValue().isNullable() );
		assertEquals(
				"long", 
				c2j.getJavaTypeName( property, false ),
				"typename should be used when rawtypemode");
		property = pc.getProperty( "otherStock" );
		assertFalse( property.getValue().isNullable() );
		assertEquals(
				"java.lang.Integer", 
				c2j.getJavaTypeName( property, false ),
				"type should still be overridden by meta attribute");
		property = pc.getIdentifierProperty();
		assertFalse( property.getValue().isNullable() );
		assertEquals( 
				"long", 
				c2j.getJavaTypeName( property, false ),
				"wrappers should be used by default");
		pc = metadata.getEntityBinding( "org.hibernate.tool.hbm2x.Hbm2JavaTest.Customer" );
		Component identifier = (Component) pc.getIdentifier();
		assertFalse(identifier.getProperties().iterator().next()
				.getValue().isNullable() );
		assertEquals( "long", c2j.getJavaTypeName( property, false ) );
	}

	@Test
	public void testExtendsImplements() {
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
        assertNull(c2j.getPOJOClass(pc).getExtends());
		POJOClass entityPOJOClass = c2j.getPOJOClass(metadata.getEntityBinding("HelloWorld" ));
		assertEquals( "Comparable", entityPOJOClass.getExtends() );
		assertNull(
				entityPOJOClass.getImplements(),
				"should be interface which cannot have implements");
		assertEquals( 
				"",
				entityPOJOClass.getImplementsDeclaration(),
				"should be interface which cannot have implements");
		PersistentClass base = new RootClass(mdbc);
		base.setClassName( "Base" );
		PersistentClass sub = new SingleTableSubclass( base, mdbc );
		sub.setClassName( "Sub" );
        assertNull(c2j.getPOJOClass(base).getExtends());
		assertEquals( "Base", c2j.getPOJOClass(sub).getExtends() );
		Map<String, MetaAttribute> m = new HashMap<>();
		MetaAttribute attribute = new MetaAttribute( "extends" );
		attribute.addValue( "x" );
		attribute.addValue( "y" );
		m.put( attribute.getName(), attribute );
		attribute = new MetaAttribute( "interface" );
		attribute.addValue( "true" );
		m.put( attribute.getName(), attribute );
		sub.setMetaAttributes( m );
		assertEquals( "Base,x,y", c2j.getPOJOClass(sub).getExtends() );
		m = new HashMap<>();
		attribute = new MetaAttribute( "implements" );
		attribute.addValue( "intf" );
		m.put( attribute.getName(), attribute );
		base.setMetaAttributes( m );
		assertEquals( "intf,java.io.Serializable", c2j.getPOJOClass(base).getImplements() );
	}

	@Test
	public void testDeclarationName() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
		PersistentClass hw = metadata.getEntityBinding( "HelloWorld" );
		POJOClass epc = c2j.getPOJOClass(pc);
		assertEquals( "Order", epc.getDeclarationName() );	
		epc = c2j.getPOJOClass(hw);
		assertEquals( "BaseHelloWorld", epc.getDeclarationName() );
	}

	@Test
	public void testAsArguments() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding(
				"org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
		assertEquals(
				"java.util.Calendar orderDate, java.math.BigDecimal total, org.hibernate.tool.hbm2x.Hbm2JavaTest.Customer customer, java.util.Collection lineItems",
				c2j.asParameterList( 
						pc.getProperties().iterator(), false, new NoopImportContext() ));
		assertEquals( 
				"orderDate, total, customer, lineItems", 
				c2j.asArgumentList( pc.getProperties().iterator() ) );
	}

	//TODO Reenable this test and make it pass (See HBX-2884)
	@Disabled
	@Test
	public void testPropertiesForFullConstructor() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding( "HelloWorld" );
		POJOClass pjc = c2j.getPOJOClass(pc);
		List<Property> wl = pjc.getPropertiesForFullConstructor();
		assertEquals( 3, wl.size() );
		PersistentClass uni = metadata.getEntityBinding( "HelloUniverse" );
		pjc = c2j.getPOJOClass(uni);
		List<Property> local = pjc.getPropertyClosureForFullConstructor();
		assertEquals( 6, local.size() );
		for(int i=0;i<wl.size();i++) {
			assertEquals(local.get( i ), wl.get( i ),  i + " position should be the same" );
		}
	}

	@Test
	public void testToString() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding( "HelloWorld" );
		POJOClass pjc = c2j.getPOJOClass(pc);
		assertTrue( pjc.needsToString() );
		Iterator<Property> iter = pjc.getToStringPropertiesIterator();
		// in HelloWorld.hbm.xml there're 2 Properties for toString
		assertEquals( "id", (iter.next() ).getName() );
		assertEquals( "hello", (iter.next() ).getName() );
		assertFalse( iter.hasNext() );
		pc = metadata.getEntityBinding( "org.hibernate.tool.hbm2x.Hbm2JavaTest.Order" );
		pjc = c2j.getPOJOClass(pc);
		assertFalse( pjc.needsToString() );
		pc = metadata.getEntityBinding( "org.hibernate.tool.hbm2x.Hbm2JavaTest.Customer" );
		Component c = (Component) pc.getProperty( "addressComponent" )
				.getValue();		
		POJOClass cc = c2j.getPOJOClass(c);
		assertTrue( cc.needsToString() );
		iter = cc.getToStringPropertiesIterator();	
		// in Customer.hbm.xml there's 1 Property for toString
		assertEquals( "city", (iter.next() ).getName() );
		assertFalse( iter.hasNext() );
	}

	@Test
	public void testImportOfSameName() {
		ImportContext ic = new ImportContextImpl("foobar");
		assertEquals("CascadeType", ic.importType("jakarta.persistence.CascadeType"));
		assertEquals("org.hibernate.annotations.CascadeType", ic.importType("org.hibernate.annotations.CascadeType"));
        assertFalse(ic.generateImports().contains("hibernate"), "The hibernate annotation should not be imported to avoid name clashes");
	}
	
	@Test
	public void testImporter() {
		ImportContext context = new ImportContextImpl( "org.hibernate" );
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
	public void testEqualsHashCode() {
		Cfg2JavaTool c2j = new Cfg2JavaTool();
		PersistentClass pc = metadata.getEntityBinding( "org.hibernate.tool.hbm2x.Hbm2JavaTest.Customer" );
		POJOClass pjc = c2j.getPOJOClass((Component) pc.getProperty("addressComponent").getValue());
		assertTrue( pjc.needsEqualsHashCode() );
        Set<String> propertySet = new HashSet<>(Arrays.asList("streetAddress1", "city", "verified"));
		Iterator<Property> iter = pjc.getEqualsHashCodePropertiesIterator();
		// iterating over all the properties to remove them all 
		assertTrue(propertySet.remove(iter.next().getName() ));
		assertTrue(propertySet.remove(iter.next().getName() ));
		assertTrue(propertySet.remove(iter.next().getName() ));
		assertTrue(propertySet.isEmpty());
		assertFalse( iter.hasNext() );
	}
	
	// TODO Re-enable this test: HBX-1249
	@Disabled
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
		File helloWorldDestination = new File(srcDir, "HelloWorld.java");
		Files.copy(helloWorldOrigin.toPath(), helloWorldDestination.toPath());
		JavaUtil.compile(genericsSource, genericsTarget);
		assertTrue(new File(genericsTarget, "HelloWorld.class").exists());
	}
	
	@Disabled
	@Test
	public void testDynamicComponent() {
		PersistentClass classMapping = 
				metadata.getEntityBinding(
						"org.hibernate.tool.hbm2x.Hbm2JavaTest.Customer");
		assertEquals(
				"java.util.Map", 
				new Cfg2JavaTool().getJavaTypeName(
						classMapping.getProperty("dynaMap"), false));
	}
	
	@Test
	public void testCapitalization() {
		assertEquals("Mail", BasicPOJOClass.beanCapitalize("Mail"));
		assertEquals("Mail", BasicPOJOClass.beanCapitalize("mail"));
		assertEquals("eMail", BasicPOJOClass.beanCapitalize("eMail"));
		assertEquals("EMail", BasicPOJOClass.beanCapitalize("EMail"));
	}
	
	@Test
	public void testUserTypes() {
		PersistentClass classMapping = metadata.getEntityBinding("org.hibernate.tool.hbm2x.Hbm2JavaTest.Customer");
		Property property = classMapping.getProperty("customDate");
		assertEquals("java.sql.Date", new Cfg2JavaTool().getJavaTypeName(property, false));	
	}
	
	private MetadataBuildingContext createMetadataBuildingContext() {
		return (MetadataBuildingContext)Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class[] { MetadataBuildingContext.class },
                (proxy, method, args) -> null);
	}
}
