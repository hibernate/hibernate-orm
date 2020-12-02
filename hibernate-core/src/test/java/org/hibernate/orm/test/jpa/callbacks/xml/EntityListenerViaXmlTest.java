/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks.xml;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/xml/MyEntity.orm.xml"
)
@SessionFactory
public class EntityListenerViaXmlTest {

	@Test
	@TestForIssue(jiraKey = "HHH-9771")
	public void testUsage(SessionFactoryScope scope) {
		JournalingListener.reset();

		scope.inTransaction(
				session -> session.persist( new MyEntity( 1, "steve" ) )
		);

		assertEquals( 1, JournalingListener.getPrePersistCount() );

		scope.inTransaction(
				session -> session.createQuery( "delete MyEntity" ).executeUpdate()
		);
	}
}
