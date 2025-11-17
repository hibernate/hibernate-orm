/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes;


import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for read-order independent resolution of user-defined types
 * Testcase for issue HHH-7300
 *
 * @author Stefan Schulze
 */
@JiraKey(value = "HHH-7300")
@BaseUnitTest
public class UserTypeMappingTest {

	private Configuration cfg;
	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setup() {
		cfg = new Configuration();
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testFirstTypeThenEntity() {
		cfg.addResource( "org/hibernate/orm/test/mapping/usertypes/TestEnumType.hbm.xml" )
				.addResource( "org/hibernate/orm/test/mapping/usertypes/TestEntity.hbm.xml" );
		try (SessionFactory sessions = cfg.buildSessionFactory( serviceRegistry )) {
			assertNotNull( sessions );
		}
	}

	@Test
	public void testFirstEntityThenType() {
		cfg.addResource( "org/hibernate/orm/test/mapping/usertypes/TestEntity.hbm.xml" )
				.addResource( "org/hibernate/orm/test/mapping/usertypes/TestEnumType.hbm.xml" );

		try (SessionFactory sessions = cfg.buildSessionFactory( serviceRegistry )) {
			assertNotNull( sessions );
		}
	}

}
