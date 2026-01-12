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
package org.hibernate.tool.hbm2x.query.QueryExporterTest;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.hibernate.tool.test.utils.ResourceUtil;
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
    private File userGroupHbmXmlFile = null;
	
	@BeforeEach
	public void setUp() throws Exception {
		JdbcUtil.createDatabase(this);
		destinationDir = new File(outputDir, "destination");
		assertTrue(destinationDir.mkdir());
        File resourcesDir = new File(outputDir, "resources");
		assertTrue(resourcesDir.mkdir());
		String[] resources = { "UserGroup.hbm.xml" };		
		ResourceUtil.createResources(this, resources, resourcesDir);
		userGroupHbmXmlFile = new File(resourcesDir, "UserGroup.hbm.xml");
		SessionFactory factory = createMetadata().buildSessionFactory();		
		Session s = factory.openSession();	
		Transaction t = s.beginTransaction();
		User user = new User("max", "jboss");
		s.persist( user );		
		user = new User("gavin", "jboss");
		s.persist( user );		
		s.flush();		
		t.commit();
		s.close();
		factory.close();		
	}
	
	@Test
	public void testQueryExporter() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.QUERY);
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(
						null, 
						new File[] { userGroupHbmXmlFile }, 
						null);
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
		final EnumSet<TargetType> targetTypes = EnumSet.noneOf( TargetType.class );
		targetTypes.add( TargetType.DATABASE );
		export.drop(targetTypes, createMetadata());		
		if (export.getExceptions() != null && !export.getExceptions().isEmpty()){
			fail("Schema export failed");
		}		
		JdbcUtil.dropDatabase(this);
	}
	
	private Metadata createMetadata() {
		Properties properties = new Properties();
		properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(
						null, 
						new File[] { userGroupHbmXmlFile }, 
						properties);
		return metadataDescriptor.createMetadata();
	}
	
}
