/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.idclass;

import javax.persistence.EntityManagerFactory;
import java.util.Arrays;

import org.hibernate.jpa.test.TestingEntityManagerFactoryGenerator;
import org.hibernate.jpa.AvailableSettings;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Alexis Bataille
 * @author Steve Ebersole
 */
public class MappedSuperclassWithEntityWithIdClassTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-5024" )
	public void testStaticMetamodel() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( ProductAttribute.class )
		);

		assertNotNull( "'ProductAttribute_.value' should not be null)", ProductAttribute_.value );
		assertNotNull( "'ProductAttribute_.owner' should not be null)", ProductAttribute_.owner );
		assertNotNull( "'ProductAttribute_.key' should not be null)", ProductAttribute_.key );

		assertNotNull( "'AbstractAttribute_.value' should not be null)", AbstractAttribute_.value );
	}

}
