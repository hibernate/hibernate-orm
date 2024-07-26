/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
