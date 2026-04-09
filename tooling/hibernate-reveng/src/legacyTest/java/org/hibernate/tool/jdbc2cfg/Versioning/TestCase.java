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
package org.hibernate.tool.jdbc2cfg.Versioning;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.hibernate.tool.test.utils.TestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * To be extended by VersioningForJDK50Test for the JPA generation part
 * @author max
 * @author koen
 */
public class TestCase extends TestTemplate {

    private static String[] CREATE_SQLS = {
            "CREATE TABLE WITH_VERSION (ONE INT, TWO INT, VERSION INT, NAME VARCHAR(256), PRIMARY KEY (ONE))",
            "CREATE TABLE NO_VERSION (ONE INT, TWO INT, NAME VARCHAR(256), PRIMARY KEY (TWO))",
            "CREATE TABLE WITH_REAL_TIMESTAMP (ONE INT, TWO INT, DBTIMESTAMP TIMESTAMP, NAME VARCHAR(256), PRIMARY KEY (ONE))",
            "CREATE TABLE WITH_FAKE_TIMESTAMP (ONE INT, TWO INT, DBTIMESTAMP INT, NAME VARCHAR(256), PRIMARY KEY (ONE))"
    };

    private static String[] DROP_SQLS = {
            "DROP TABLE WITH_VERSION",
            "DROP TABLE NO_VERSION",
            "DROP TABLE WITH_REAL_TIMESTAMP",
            "DROP TABLE WITH_FAKE_TIMESTAMP"
    };

    private static String[] HIBERNATE_PROPERTIES = {
            "hibernate.connection.url",
            "hibernate.connection.username",
    };
	
	private Metadata metadata = null;
	private MetadataDescriptor metadataDescriptor = null;
	
	@TempDir
	public File outputFolder = new File("output");
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);
		metadata = metadataDescriptor
				.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testVersion() {		
		PersistentClass cl = metadata.getEntityBinding("WithVersion");		
		Property version = cl.getVersion();
		assertNotNull(version);
		assertEquals("version", version.getName());		
		cl = metadata.getEntityBinding("NoVersion");
		assertNotNull(cl);
		version = cl.getVersion();
		assertNull(version);		
	}
	
	@Test
	public void testGenerateMappings() {
        Exporter exporter = new HbmExporter();		
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
 		exporter.start();		
		File[] files = new File[4];		
		files[0] = new File(outputFolder, "WithVersion.hbm.xml");
		files[1] = new File(outputFolder, "NoVersion.hbm.xml");
		files[2] = new File(outputFolder, "WithRealTimestamp.hbm.xml");
		files[3] = new File(outputFolder, "WithFakeTimestamp.hbm.xml");		
		Metadata metadata = MetadataDescriptorFactory
				.createNativeDescriptor(null, files, null)
				.createMetadata();
		PersistentClass cl = metadata.getEntityBinding( "WithVersion" );				
		Property version = cl.getVersion();
		assertNotNull(version);
		assertEquals("version", version.getName());	
		cl = metadata.getEntityBinding( "NoVersion" );
		assertNotNull(cl);
		version = cl.getVersion();
		assertNull(version);
		cl = metadata.getEntityBinding( "WithRealTimestamp" );
		assertNotNull(cl);
		version = cl.getVersion();
		assertNotNull(version);
		assertEquals("timestamp", version.getType().getName());
		cl = metadata.getEntityBinding( "WithFakeTimestamp" );
		assertNotNull(cl);
		version = cl.getVersion();
		assertNotNull(version);
		assertEquals("integer", version.getType().getName());
	}
    
}
