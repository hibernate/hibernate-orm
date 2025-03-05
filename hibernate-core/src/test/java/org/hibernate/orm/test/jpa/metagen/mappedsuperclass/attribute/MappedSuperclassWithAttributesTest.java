/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.attribute;

import java.util.Arrays;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.TestingEntityManagerFactoryGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-5024" )
@BaseUnitTest
public class MappedSuperclassWithAttributesTest {
	@Test
	public void testStaticMetamodel() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( Product.class )
		);
		try {
			assertNotNull( Product_.id, "'Product_.id' should not be null)" );
			assertNotNull( Product_.name, "'Product_.name' should not be null)" );

			assertNotNull( AbstractNameable_.name, "'AbstractNameable_.name' should not be null)" );
		}
		finally {
			emf.close();
		}
	}
}
