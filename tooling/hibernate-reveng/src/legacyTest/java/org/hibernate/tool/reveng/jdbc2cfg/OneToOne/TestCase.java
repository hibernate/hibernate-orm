/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.OneToOne;

import jakarta.persistence.Persistence;
import org.hibernate.MappingException;
import org.hibernate.Version;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.internal.metadata.NativeMetadataDescriptor;
import org.hibernate.tool.reveng.internal.core.util.EnhancedValue;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JavaUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.hibernate.tool.reveng.test.utils.SchemaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	private MetadataDescriptor metadataDescriptor = null;
	private Metadata metadata = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadataDescriptor = MetadataDescriptorFactory.createReverseEngineeringDescriptor(null, null);
		metadata = metadataDescriptor.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testOneToOneSingleColumnBiDirectional() {
		PersistentClass person = metadata.getEntityBinding("Person");
		Property addressProperty = person.getProperty("addressPerson");
		assertNotNull(addressProperty);
		assertInstanceOf(OneToOne.class, addressProperty.getValue());
		OneToOne oto = (OneToOne) addressProperty.getValue();
		assertEquals(1, oto.getColumnSpan());
		assertEquals("Person", oto.getEntityName());
		assertEquals("AddressPerson", oto.getReferencedEntityName());
		assertEquals(2, person.getPropertyClosureSpan());
		assertEquals("personId", person.getIdentifierProperty().getName());
		assertFalse(oto.isConstrained());
		PersistentClass addressPerson = metadata.getEntityBinding("AddressPerson");
		Property personProperty = addressPerson.getProperty("person");
		assertNotNull(personProperty);
		assertInstanceOf(OneToOne.class, personProperty.getValue());
		oto = (OneToOne) personProperty.getValue();
		assertTrue(oto.isConstrained());
		assertEquals(1, oto.getColumnSpan());
		assertEquals("AddressPerson", oto.getEntityName());
		assertEquals("Person", oto.getReferencedEntityName());
		assertEquals(2, addressPerson.getPropertyClosureSpan());
		assertEquals("addressId", addressPerson.getIdentifierProperty().getName());
	}

	@Test
	public void testAddressWithForeignKeyGeneration() {
		PersistentClass address = metadata.getEntityBinding("AddressPerson");
		assertEquals("foreign", ((EnhancedValue)address.getIdentifier()).getIdentifierGeneratorStrategy());
	}

	@Test
	public void testOneToOneMultiColumnBiDirectional() {
		PersistentClass person = metadata.getEntityBinding("MultiPerson");
		Property addressProperty = person.getProperty("addressMultiPerson");
		assertNotNull(addressProperty);
		assertInstanceOf(OneToOne.class, addressProperty.getValue());
		OneToOne oto = (OneToOne) addressProperty.getValue();
		assertEquals(2, oto.getColumnSpan());
		assertEquals("MultiPerson", oto.getEntityName());
		assertEquals("AddressMultiPerson", oto.getReferencedEntityName());
		assertFalse(oto.isConstrained());
		assertEquals(2, person.getPropertyClosureSpan());
		assertEquals("id", person.getIdentifierProperty().getName(), "compositeid gives generic id name");
		PersistentClass addressPerson = metadata.getEntityBinding("AddressMultiPerson");
		Property personProperty = addressPerson.getProperty("multiPerson");
		assertNotNull(personProperty);
		assertInstanceOf(OneToOne.class, personProperty.getValue());
		oto = (OneToOne) personProperty.getValue();
		assertEquals(2, oto.getColumnSpan());
		assertEquals("AddressMultiPerson", oto.getEntityName());
		assertEquals("MultiPerson", oto.getReferencedEntityName());
		assertEquals(2, addressPerson.getPropertyClosureSpan());
		assertEquals("id", addressPerson.getIdentifierProperty().getName(), "compositeid gives generic id name");
		assertTrue(oto.isConstrained());
	}

	@Test
	public void testBuildMappings() {
		assertNotNull(metadata);
	}

	@Test
	public void testGenerateMappingAndReadable() throws MalformedURLException {
		HbmExporter hme = new HbmExporter();
		hme.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hme.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		hme.start();
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "Person.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "AddressPerson.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "AddressMultiPerson.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "MultiPerson.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "MiddleTable.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "LeftTable.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "RightTable.hbm.xml") );
		assertEquals(7, Objects.requireNonNull(outputDir.listFiles()).length);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[0]);
		exporter.getProperties().setProperty("ejb3", "false");
		exporter.getProperties().setProperty("jdk5", "false");
		exporter.start();
		JavaUtil.compile(outputDir);
		URL[] urls = new URL[] { outputDir.toURI().toURL() };
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader ucl = new URLClassLoader(urls, oldLoader );
		try {
			Thread.currentThread().setContextClassLoader(ucl);
			StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
			ServiceRegistry serviceRegistry = builder.build();
			File[] files = new File[7];
			files[0] = new File(outputDir, "Person.hbm.xml");
			files[1] = new File(outputDir, "AddressPerson.hbm.xml");
			files[2] = new File(outputDir, "AddressMultiPerson.hbm.xml");
			files[3] = new File(outputDir, "MultiPerson.hbm.xml");
			files[4] = new File(outputDir, "MiddleTable.hbm.xml");
			files[5] = new File(outputDir, "LeftTable.hbm.xml");
			files[6] = new File(outputDir, "RightTable.hbm.xml");
			SchemaUtil.validateSchema(
					MetadataDescriptorFactory
						.createNativeDescriptor(null, files, null)
						.createMetadata(),
					serviceRegistry);
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}

	@Test
	public void testGenerateAnnotatedClassesAndReadable() throws MappingException, ClassNotFoundException, MalformedURLException {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[0]);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.getProperties().setProperty("jdk5", "true");
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "Person.java") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "AddressPerson.java") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "MultiPersonId.java") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "AddressMultiPerson.java") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "AddressMultiPersonId.java") );
		JUnitUtil.assertIsNonEmptyFile( new File(outputDir, "MultiPerson.java") );
		assertEquals(9, Objects.requireNonNull(outputDir.listFiles()).length);
		List<String> paths = new ArrayList<>();
		paths.add(JavaUtil.resolvePathToJarFileFor(Persistence.class)); // for jpa api
		paths.add(JavaUtil.resolvePathToJarFileFor(Version.class)); // for hibernate core
		JavaUtil.compile(outputDir, paths);
		URL[] urls = new URL[] { outputDir.toURI().toURL() };
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader ucl = new URLClassLoader(urls, oldLoader );
		Class<?> personClass = ucl.loadClass("Person");
		Class<?> multiPersonClass = ucl.loadClass("MultiPerson");
		Class<?> addressMultiPerson = ucl.loadClass("AddressMultiPerson");
		Class<?> addressMultiPersonId = ucl.loadClass("AddressMultiPersonId");
		Class<?> addressPerson = ucl.loadClass("AddressPerson");
		Class<?> multiPersonIdClass = ucl.loadClass("MultiPersonId");
		Class<?> middleClass = ucl.loadClass("MiddleTable");
		Class<?> rightClass = ucl.loadClass("LeftTable");
		Class<?> leftClass = ucl.loadClass("RightTable");
		try {
			Thread.currentThread().setContextClassLoader(ucl);
			StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
			ServiceRegistry serviceRegistry = builder.build();
			NativeMetadataDescriptor mds = new NativeMetadataDescriptor(null, null, null);
			HibernateUtil.addAnnotatedClass(mds, personClass);
			HibernateUtil.addAnnotatedClass(mds, multiPersonClass);
			HibernateUtil.addAnnotatedClass(mds, addressMultiPerson);
			HibernateUtil.addAnnotatedClass(mds, addressMultiPersonId);
			HibernateUtil.addAnnotatedClass(mds, addressPerson);
			HibernateUtil.addAnnotatedClass(mds, multiPersonIdClass);
			HibernateUtil.addAnnotatedClass(mds, middleClass);
			HibernateUtil.addAnnotatedClass(mds, rightClass);
			HibernateUtil.addAnnotatedClass(mds, leftClass);
			Metadata metadata = mds.createMetadata();
			SchemaUtil.validateSchema(metadata, serviceRegistry);
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}

}
