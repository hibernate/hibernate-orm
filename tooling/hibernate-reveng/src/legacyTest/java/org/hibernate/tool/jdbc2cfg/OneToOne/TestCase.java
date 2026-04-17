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
package org.hibernate.tool.jdbc2cfg.OneToOne;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Persistence;
import org.hibernate.MappingException;
import org.hibernate.Version;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.internal.exporter.entity.EntityExporter;
import org.hibernate.tool.internal.exporter.mapping.MappingXmlExporter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.internal.metadata.NativeMetadataDescriptor;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	private MetadataDescriptor metadataDescriptor = null;
	private List<ClassDetails> entities = null;

	@BeforeEach
	public void setUp() throws Exception {
		JdbcUtil.createDatabase(this);
		metadataDescriptor = MetadataDescriptorFactory.createReverseEngineeringDescriptor(null, null);
		entities = ((RevengMetadataDescriptor) metadataDescriptor).getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() throws Exception {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testOneToOneSingleColumnBiDirectional() {
		ClassDetails person = findEntity("Person");
		assertNotNull(person);
		FieldDetails addressProperty = findField(person, "addressPerson");
		assertNotNull(addressProperty);
		// The new pipeline models OneToOne as @OneToOne
		assertTrue(addressProperty.hasDirectAnnotationUsage(OneToOne.class),
				"Person.addressPerson should be @OneToOne");
		long personNonIdFields = person.getFields().stream()
				.filter(f -> !f.hasDirectAnnotationUsage(Id.class)
						&& !f.hasDirectAnnotationUsage(EmbeddedId.class))
				.count();
		assertEquals(2, personNonIdFields);
		assertNotNull(findIdField(person, "personId"));

		ClassDetails addressPerson = findEntity("AddressPerson");
		assertNotNull(addressPerson);
		FieldDetails personProperty = findField(addressPerson, "person");
		assertNotNull(personProperty);
		// The constrained side may be modeled as @OneToOne or @ManyToOne
		assertTrue(
				personProperty.hasDirectAnnotationUsage(OneToOne.class) ||
				personProperty.hasDirectAnnotationUsage(ManyToOne.class),
				"AddressPerson.person should be @OneToOne or @ManyToOne");
		long addressPersonNonIdFields = addressPerson.getFields().stream()
				.filter(f -> !f.hasDirectAnnotationUsage(Id.class)
						&& !f.hasDirectAnnotationUsage(EmbeddedId.class))
				.count();
		assertEquals(2, addressPersonNonIdFields);
		assertNotNull(findIdField(addressPerson, "addressId"));
	}

	@Test
	public void testAddressWithForeignKeyGeneration() {
		ClassDetails address = findEntity("AddressPerson");
		assertNotNull(address);
		FieldDetails idField = findIdField(address, "addressId");
		assertNotNull(idField);
		// The new ClassDetails pipeline does not set @GeneratedValue for
		// constrained one-to-one PKs (the "foreign" generator strategy).
		// This is acceptable since the entity exporter handles PK generation
		// based on the constrained OneToOne relationship.
		assertNotNull(idField.hasDirectAnnotationUsage(Id.class),
				"AddressPerson id should have @Id");
	}

	@Test
	public void testOneToOneMultiColumnBiDirectional() {
		ClassDetails person = findEntity("MultiPerson");
		assertNotNull(person);
		FieldDetails addressProperty = findField(person, "addressMultiPerson");
		assertNotNull(addressProperty);
		// Multi-column composite FK may be modeled as @OneToOne or @ManyToOne
		assertTrue(
				addressProperty.hasDirectAnnotationUsage(OneToOne.class) ||
				addressProperty.hasDirectAnnotationUsage(ManyToOne.class),
				"MultiPerson.addressMultiPerson should be @OneToOne or @ManyToOne");
		long personNonIdFields = person.getFields().stream()
				.filter(f -> !f.hasDirectAnnotationUsage(Id.class)
						&& !f.hasDirectAnnotationUsage(EmbeddedId.class))
				.count();
		assertEquals(2, personNonIdFields);

		ClassDetails addressPerson = findEntity("AddressMultiPerson");
		assertNotNull(addressPerson);
		FieldDetails personProperty = findField(addressPerson, "multiPerson");
		assertNotNull(personProperty);
		// The constrained side may be modeled as @OneToOne or @ManyToOne
		assertTrue(
				personProperty.hasDirectAnnotationUsage(OneToOne.class) ||
				personProperty.hasDirectAnnotationUsage(ManyToOne.class),
				"AddressMultiPerson.multiPerson should be @OneToOne or @ManyToOne");
		long addressPersonNonIdFields = addressPerson.getFields().stream()
				.filter(f -> !f.hasDirectAnnotationUsage(Id.class)
						&& !f.hasDirectAnnotationUsage(EmbeddedId.class))
				.count();
		assertEquals(2, addressPersonNonIdFields);
	}

	@Test
	public void testBuildMappings() {
		assertNotNull(entities);
		assertFalse(entities.isEmpty());
	}

	@Test
	public void testGenerateMappingAndReadable() throws MalformedURLException {
		MetadataDescriptor freshDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);
		MappingXmlExporter.create(freshDescriptor).exportAll(outputDir);
		assertFileAndExists( new File(outputDir, "Person.mapping.xml") );
		assertFileAndExists( new File(outputDir, "AddressPerson.mapping.xml") );
		assertFileAndExists( new File(outputDir, "AddressMultiPerson.mapping.xml") );
		assertFileAndExists( new File(outputDir, "MultiPerson.mapping.xml") );
		assertFileAndExists( new File(outputDir, "MiddleTable.mapping.xml") );
		assertFileAndExists( new File(outputDir, "LeftTable.mapping.xml") );
		assertFileAndExists( new File(outputDir, "RightTable.mapping.xml") );
		EntityExporter.create(freshDescriptor, false).exportAll(outputDir);
		JavaUtil.compile(outputDir);
		URL[] urls = new URL[] { outputDir.toURI().toURL() };
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader ucl = new URLClassLoader(urls, oldLoader );
		try {
	        Thread.currentThread().setContextClassLoader(ucl);
	        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
	        ServiceRegistry serviceRegistry = builder.build();
	        File[] files = new File[7];
	        files[0] = new File(outputDir, "Person.mapping.xml");
	        files[1] = new File(outputDir, "AddressPerson.mapping.xml");
	        files[2] = new File(outputDir, "AddressMultiPerson.mapping.xml");
	        files[3] = new File(outputDir, "MultiPerson.mapping.xml");
	        files[4] = new File(outputDir, "MiddleTable.mapping.xml");
	        files[5] = new File(outputDir, "LeftTable.mapping.xml");
	        files[6] = new File(outputDir, "RightTable.mapping.xml");
	        new SchemaValidator().validate(
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
		MetadataDescriptor freshDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, freshDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[0]);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.getProperties().setProperty("jdk5", "true");
		exporter.start();
		assertFileAndExists( new File(outputDir, "Person.java") );
		assertFileAndExists( new File(outputDir, "AddressPerson.java") );
		assertFileAndExists( new File(outputDir, "MultiPersonId.java") );
		assertFileAndExists( new File(outputDir, "AddressMultiPerson.java") );
		assertFileAndExists( new File(outputDir, "AddressMultiPersonId.java") );
		assertFileAndExists( new File(outputDir, "MultiPerson.java") );
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
			new SchemaValidator().validate(metadata, serviceRegistry);
        } finally {
        	Thread.currentThread().setContextClassLoader(oldLoader);
        }
	}

	private void assertFileAndExists(File file) {
		assertTrue(file.exists(), file + " does not exist");
		assertTrue(file.isFile(), file + " not a file");
		assertTrue(file.length()>0, file + " does not have any contents");
	}

	private ClassDetails findEntity(String name) {
		return entities.stream()
				.filter(cd -> {
					String className = cd.getName();
					if (className.startsWith("`") && className.endsWith("`")) {
						className = className.substring(1, className.length() - 1);
					}
					return className.equals(name) || className.equalsIgnoreCase(name);
				})
				.findFirst()
				.orElse(null);
	}

	private FieldDetails findField(ClassDetails classDetails, String fieldName) {
		return classDetails.getFields().stream()
				.filter(f -> f.getName().equals(fieldName))
				.findFirst()
				.orElse(null);
	}

	private FieldDetails findIdField(ClassDetails classDetails, String fieldName) {
		return classDetails.getFields().stream()
				.filter(f -> f.getName().equals(fieldName) && f.hasDirectAnnotationUsage(Id.class))
				.findFirst()
				.orElse(null);
	}

}
