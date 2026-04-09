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
package org.hibernate.tool.jdbc2cfg.ManyToMany;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {
	
	@TempDir
	public File outputDir = new File("output");
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testNoManyToManyBiDirectional() {
        
        AbstractStrategy c = new DefaultStrategy();
        c.setSettings(new RevengSettings(c).setDetectManyToMany(false)); 
        Metadata metadata =  MetadataDescriptorFactory
        		.createReverseEngineeringDescriptor(c, null)
        		.createMetadata();

        PersistentClass project = metadata.getEntityBinding("Project");
		
		assertNotNull(project.getProperty("worksOns"));
		//assertNotNull(project.getProperty("employee"));
		assertEquals(3, project.getPropertyClosureSpan());		
		assertEquals("projectId", project.getIdentifierProperty().getName());
		
		PersistentClass employee = metadata.getEntityBinding("Employee");
		
		assertNotNull(employee.getProperty("worksOns"));
		assertNotNull(employee.getProperty("employees"));
		assertNotNull(employee.getProperty("employee"));
		//assertNotNull(employee.getProperty("projects"));
		assertEquals(6, employee.getPropertyClosureSpan());
		assertEquals("id", employee.getIdentifierProperty().getName());
		
		PersistentClass worksOn = metadata.getEntityBinding("WorksOn");
		
		assertNotNull(worksOn.getProperty("project"));
		assertNotNull(worksOn.getProperty("employee"));
		assertEquals(2, worksOn.getPropertyClosureSpan());
		assertEquals("id", worksOn.getIdentifierProperty().getName());		
	}
	
	@Test
	public void testAutoCreation() {
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
		
        assertNull(metadata.getEntityBinding( "WorksOn" ), "No middle class should be generated.");
        
        assertNotNull(metadata.getEntityBinding( "WorksOnContext" ), "Should create worksontext since one of the foreign keys is not part of pk");
        
        PersistentClass projectClass = metadata.getEntityBinding("Project");
		assertNotNull( projectClass );

		PersistentClass employeeClass = metadata.getEntityBinding("Employee");
		assertNotNull( employeeClass );

		try {
			projectClass.getProperty("worksOns");
			fail("property worksOns should not exist on " + projectClass);
		} catch(MappingException ignored) {}
		try {
			employeeClass.getProperty("worksOns");
			fail("property worksOns should not exist on " + employeeClass);
		} catch(MappingException ignored) {}

        Property property = employeeClass.getProperty( "projects" );
		assertNotNull( property);
		assertNotNull( projectClass.getProperty( "employees" ));				
		
	}

	@Test
	public void testFalsePositive() {
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();	    
        assertNotNull(metadata.getEntityBinding( "NonMiddle" ), "Middle class should be generated.");	
	}

	@Test
	public void testBuildMappings() {		
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
		assertNotNull(metadata);
	}
	
	@Test
	public void testGenerateAndReadable() {
		
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);

		assertNotNull(metadataDescriptor.createMetadata());
		
		HbmExporter hme = new HbmExporter();
		hme.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hme.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		hme.start();		
		
		assertFileAndExists( new File(outputDir, "Employee.hbm.xml") );
		assertFileAndExists( new File(outputDir, "Project.hbm.xml") );
		assertFileAndExists( new File(outputDir, "WorksOnContext.hbm.xml") );
		
		assertFileAndExists( new File(outputDir, "RightTable.hbm.xml") );
		assertFileAndExists( new File(outputDir, "LeftTable.hbm.xml") );
		assertFileAndExists( new File(outputDir, "NonMiddle.hbm.xml") ); //Must be there since it has a fkey that is not part of the pk
		
		assertFalse(new File(outputDir, "WorksOn.hbm.xml").exists() );
		
		assertEquals(6, Objects.requireNonNull(outputDir.listFiles()).length);
		
		File[] files = new File[3];
		files[0] = new File(outputDir, "Employee.hbm.xml");
		files[1] = new File(outputDir, "Project.hbm.xml");
		files[2] = new File(outputDir, "WorksOnContext.hbm.xml");
		
		assertNotNull(MetadataDescriptorFactory
				.createNativeDescriptor(null, files, null)
				.createMetadata());
		
	}
	

	private void assertFileAndExists(File file) {
		assertTrue(file.exists(), file + " does not exist");
		assertTrue(file.isFile(), file + " not a file");		
		assertTrue(file.length()>0, file + " does not have any contents");
	}

}
