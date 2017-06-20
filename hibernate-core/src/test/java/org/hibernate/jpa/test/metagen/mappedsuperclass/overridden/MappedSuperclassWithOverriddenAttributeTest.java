/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.overridden;

import java.util.Arrays;
import javax.persistence.EntityManagerFactory;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.TestingEntityManagerFactoryGenerator;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Oliver Breidenbach
 */
@TestForIssue(jiraKey = "HHH-11078")
public class MappedSuperclassWithOverriddenAttributeTest
		extends BaseUnitTestCase {

	@Test
	@FailureExpected(jiraKey = "HHH-11078")
	public void testStaticMetamodelOverridden() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( Product2.class )
		);
		try {
			assertNotNull(
					"'Product1_.overridenName' should not be null)",
					Product1_.overridenName
			);

			assertNotNull(
					"'Product2_.overridenName' should not be null)",
					Product2_.overridenName
			); // is null
		}
		finally {
			emf.close();
		}
	}
}
