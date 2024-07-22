/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/inheritance/discriminator/joinedsubclass/TestEntity.hbm.xml"
)
@SessionFactory
public class JoinedSubclassWithRootInterfaceTest {

	@Test
	@TestForIssue(jiraKey = "HHH-11554")
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntityImpl testEntity = new TestEntityImpl();
			testEntity.setId( 1 );
			session.persist( testEntity );
		} );
	}
}
