/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.idclass;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;

import org.hibernate.orm.test.jpa.TestingEntityManagerFactoryGenerator;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Alexis Bataille
 * @author Steve Ebersole
 */
@BaseUnitTest
public class MappedSuperclassWithEntityWithIdClassTest {
	@Test
	@JiraKey( value = "HHH-5024" )
	public void testStaticMetamodel() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( ProductAttribute.class )
		);

		try {
			assertNotNull( ProductAttribute_.value, "'ProductAttribute_.value' should not be null)" );
			assertNotNull( ProductAttribute_.owner, "'ProductAttribute_.owner' should not be null)" );
			assertNotNull( ProductAttribute_.key, "'ProductAttribute_.key' should not be null)" );

			assertNotNull( AbstractAttribute_.value, "'AbstractAttribute_.value' should not be null)" );
		}
		finally {
			emf.close();
		}
	}

}
