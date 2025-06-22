/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.embeddedid;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.TestingEntityManagerFactoryGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class MappedSuperclassWithEmbeddedIdTest {
	@Test
	@JiraKey( value = "HHH-5024" )
	public void testStaticMetamodel() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( Product.class )
		);

		try {
			assertNotNull( Product_.description, "'Product_.description' should not be null)" );
			assertNotNull( Product_.id, "'Product_.id' should not be null)" );

			assertNotNull( AbstractProduct_.id, "'AbstractProduct_.id' should not be null)" );

			assertNotNull( ProductId_.id, "'ProductId_.id' should not be null)" );
			assertNotNull( ProductId_.code, "'ProductId_.code' should not be null)" );
		}
		finally {
			emf.close();
		}
	}
}
