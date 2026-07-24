/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.query.QueryExporterTest;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	private File destinationDir = null;

	@BeforeEach
	public void setUp() throws Exception {
		JdbcUtil.createDatabase(this);
		destinationDir = new File(outputDir, "destination");
		assertTrue(destinationDir.mkdir());
		SessionFactory factory = SessionFactoryPipeline.build( createMetadata(), new SessionFactoryOptionsCollector() );
		Session s = factory.openSession();
		Transaction t = s.beginTransaction();
		User user = new User("max", "jboss");
		s.persist(user);
		user = new User("gavin", "jboss");
		s.persist(user);
		s.flush();
		t.commit();
		s.close();
		factory.close();
	}

	@Test
	public void testQueryExporter() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.QUERY);
		MetadataDescriptor metadataDescriptor = createMetadataDescriptor(null);
		exporter.getProperties().put(AvailableSettings.HBM2DDL_AUTO, "update");
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, destinationDir);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, "queryresult.txt");
		List<String> queries = new ArrayList<>();
		queries.add("from java.lang.Object");
		exporter.getProperties().put(ExporterConstants.QUERY_LIST, queries);
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(destinationDir, "queryresult.txt"));
	}

	@AfterEach
	public void tearDown() throws Exception {
		SchemaExport export = new SchemaExport();
		final EnumSet<TargetType> targetTypes = EnumSet.noneOf(TargetType.class);
		targetTypes.add(TargetType.DATABASE);
		export.drop(targetTypes, createMetadata());
		if (export.getExceptions() != null && !export.getExceptions().isEmpty()) {
			fail("Schema export failed");
		}
		JdbcUtil.dropDatabase(this);
	}

	private MetadataDescriptor createMetadataDescriptor(Properties properties) {
		Properties props = properties != null ? properties : new Properties();
		props.put(AvailableSettings.HBM2DDL_AUTO, "update");
		MetadataDescriptor md = MetadataDescriptorFactory.createNativeDescriptor(null, null, props);
		HibernateUtil.addAnnotatedClass(md, User.class);
		return md;
	}

	private Metadata createMetadata() {
		return createMetadataDescriptor(null).createMetadata();
	}

}
