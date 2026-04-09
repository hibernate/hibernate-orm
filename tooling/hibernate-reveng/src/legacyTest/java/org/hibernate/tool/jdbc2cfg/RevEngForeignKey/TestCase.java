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
package org.hibernate.tool.jdbc2cfg.RevEngForeignKey;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {
	
	private static final String FOREIGN_KEY_TEST_XML = "org/hibernate/tool/jdbc2cfg/RevEngForeignKey/foreignkeytest.reveng.xml";
	private static final String BAD_FOREIGN_KEY_XML = "org/hibernate/tool/jdbc2cfg/RevEngForeignKey/badforeignkeytest.reveng.xml";
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testDefaultBiDirectional() {
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
		PersistentClass project = metadata.getEntityBinding("Project");
		assertNotNull(project.getProperty("worksOns"));
		assertNotNull(project.getProperty("employee"));
		assertEquals(3, project.getPropertyClosureSpan());		
		assertEquals("projectId", project.getIdentifierProperty().getName());
		PersistentClass employee = metadata.getEntityBinding("Employee");
		assertNotNull(employee.getProperty("worksOns"));
		assertNotNull(employee.getProperty("employees"));
		assertNotNull(employee.getProperty("employee"));
		assertNotNull(employee.getProperty("projects"));
		assertEquals(5, employee.getPropertyClosureSpan());
		assertEquals("id", employee.getIdentifierProperty().getName());
		PersistentClass worksOn = metadata.getEntityBinding("WorksOn");
		assertNotNull(worksOn.getProperty("project"));
		assertNotNull(worksOn.getProperty("employee"));
		assertEquals(4, worksOn.getPropertyClosureSpan());
		assertEquals("id", worksOn.getIdentifierProperty().getName());	
	}

	@Test
	public void testSetAndManyToOne() {
		OverrideRepository or = new OverrideRepository();
		or.addResource(FOREIGN_KEY_TEST_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(new DefaultStrategy());
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(repository, null)
				.createMetadata();			
		PersistentClass project = metadata.getEntityBinding("Project");		
		assertNotNull(project.getProperty("worksOns"));
		assertPropertyNotExists(project, "employee", "should be removed by reveng.xml");
		Property property = project.getProperty("teamLead");
		assertNotNull(property);
        assertInstanceOf(SimpleValue.class, property.getValue());
		assertEquals(3, project.getPropertyClosureSpan());		
		assertEquals("projectId", project.getIdentifierProperty().getName());
		PersistentClass employee = metadata.getEntityBinding("Employee");	
		assertNotNull(employee.getProperty("worksOns"));
		assertNotNull(employee.getProperty("manager"), "property should be renamed by reveng.xml");		
		assertPropertyNotExists( employee, "employees", "set should be excluded by reveng.xml" );
		Property setProperty = employee.getProperty("managedProjects");
		assertNotNull(setProperty, "should be renamed by reveng.xml");
		assertEquals("delete, update", setProperty.getCascade());
		assertEquals(4, employee.getPropertyClosureSpan());
		assertEquals("id", employee.getIdentifierProperty().getName());
		PersistentClass worksOn = metadata.getEntityBinding("WorksOn");
		assertNotNull(worksOn.getProperty("project"));
		assertNotNull(worksOn.getProperty("employee"));
		assertEquals(4, worksOn.getPropertyClosureSpan());
		assertEquals("id", worksOn.getIdentifierProperty().getName());
	}

	@Test
	public void testOneToOne() {
		OverrideRepository or = new OverrideRepository();
		or.addResource(FOREIGN_KEY_TEST_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(new DefaultStrategy());
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(repository, null)
				.createMetadata();
		PersistentClass person = metadata.getEntityBinding("Person");
		PersistentClass addressPerson = metadata.getEntityBinding("AddressPerson");
		PersistentClass addressMultiPerson = metadata.getEntityBinding("AddressMultiPerson");
		PersistentClass multiPerson = metadata.getEntityBinding("MultiPerson");	
		assertPropertyNotExists(addressPerson, "person", "should be removed by reveng.xml");
		assertPropertyNotExists(person, "addressPerson", "should be removed by reveng.xml");	
		Property property = addressMultiPerson.getProperty("renamedOne");
		assertNotNull(property);	
		assertEquals("delete", property.getCascade(), "Cascade should be set to delete by reveng.xml");
		assertPropertyNotExists(multiPerson, "addressMultiPerson", "should not be there");
		Property o2o = multiPerson.getProperty("renamedInversedOne");
		assertNotNull(o2o);
		assertEquals("update", o2o.getCascade());
		assertEquals("JOIN", o2o.getValue().getFetchMode().toString());
	}
	
	@Test
	public void testDuplicateForeignKeyDefinition() {
		try {
			OverrideRepository or = new OverrideRepository();
			or.addResource(BAD_FOREIGN_KEY_XML);
			RevengStrategy repository = or.getReverseEngineeringStrategy(new DefaultStrategy());
			MetadataDescriptorFactory
					.createReverseEngineeringDescriptor(repository, null)
					.createMetadata();
			fail("Should fail because foreign key is already defined in the database"); // maybe we should ignore the definition and only listen to what is overwritten ? For now, we error.
		} catch(MappingException me) {
			assertTrue(me.getMessage().contains("already defined"));
		}		
	}

	@Test
	public void testManyToOneAttributeDefaults() {	
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null)
				.createMetadata();
		PersistentClass classMapping = metadata.getEntityBinding("Employee");
		Property property = classMapping.getProperty("employee");	
		assertEquals("none", property.getCascade());
        assertTrue(property.isUpdatable());
        assertTrue(property.isInsertable());
		assertEquals("SELECT", property.getValue().getFetchMode().toString());
	}
	
	@Test
	public void testManyToOneAttributeOverrides() {
		OverrideRepository or = new OverrideRepository();	
		or.addResource(FOREIGN_KEY_TEST_XML);
		RevengStrategy repository = or.getReverseEngineeringStrategy(new DefaultStrategy());
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(repository, null)
				.createMetadata();
		PersistentClass classMapping = metadata.getEntityBinding("Employee");
		Property property = classMapping.getProperty("manager");	
		assertEquals("all", property.getCascade());
        assertFalse(property.isUpdatable());
        assertFalse(property.isInsertable());
		assertEquals("JOIN", property.getValue().getFetchMode().toString());
	}	

	private void assertPropertyNotExists(PersistentClass employee, String name, String msg) {
		try {
			employee.getProperty(name);
			fail(msg);
		} catch(MappingException ignored) {}
	}

}
