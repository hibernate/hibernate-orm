/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.ManyToMany;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.internal.core.strategy.AbstractStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author max
 * @author koen
 */
public class TestCase {

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

}
