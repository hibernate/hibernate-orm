/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks.xml;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@Jpa(
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/xml/MyEntity.orm.xml"
)
public class EntityListenerViaXmlTest {

	@Test
	@TestForIssue(jiraKey = "HHH-9771")
	public void testUsage(EntityManagerFactoryScope scope) {
		JournalingListener.reset();

		scope.inTransaction(
				entityManager -> entityManager.persist( new MyEntity( 1, "steve" ) )
		);

		assertEquals( 1, JournalingListener.getPrePersistCount() );

		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete MyEntity" ).executeUpdate()
		);
	}
}
