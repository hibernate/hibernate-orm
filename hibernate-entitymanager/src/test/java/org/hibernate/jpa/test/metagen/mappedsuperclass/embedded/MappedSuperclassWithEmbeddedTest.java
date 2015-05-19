/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.embedded;

import javax.persistence.EntityManagerFactory;
import java.util.Arrays;

import org.hibernate.jpa.test.TestingEntityManagerFactoryGenerator;
import org.hibernate.jpa.AvailableSettings;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassWithEmbeddedTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-5024" )
	public void testStaticMetamodel() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( Company.class )
		);

		assertNotNull( "'Company_.id' should not be null)", Company_.id );
		assertNotNull( "'Company_.address' should not be null)", Company_.address );

		assertNotNull( "'AbstractAddressable_.address' should not be null)", AbstractAddressable_.address );

		emf.close();
	}
}
