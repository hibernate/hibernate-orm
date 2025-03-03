/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.embedded;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;

import org.hibernate.orm.test.jpa.TestingEntityManagerFactoryGenerator;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class MappedSuperclassWithEmbeddedTest {
	@Test
	@JiraKey( value = "HHH-5024" )
	public void testStaticMetamodel() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( Company.class )
		);
		try {
			assertNotNull( Company_.id, "'Company_.id' should not be null)" );
			assertNotNull( Company_.address, "'Company_.address' should not be null)" );

			assertNotNull( AbstractAddressable_.address, "'AbstractAddressable_.address' should not be null)" );
		}
		finally {
			emf.close();
		}
	}
}
