/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.various;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the @OptimisticLock annotation.
 *
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = Conductor.class
)
@SessionFactory
public class OptimisticLockAnnotationTest {

	@Test
	public void testOptimisticLockExcludeOnNameProperty(SessionFactoryScope scope) {


		scope.inTransaction(
				session -> {
					Conductor c = new Conductor();
					c.setName( "Bob" );
					session.persist( c );
					session.flush();

					session.clear();

					c = session.get( Conductor.class, c.getId() );
					Long version = c.getVersion();
					c.setName( "Don" );
					session.flush();

					session.clear();

					c = session.get( Conductor.class, c.getId() );
					assertEquals( version, c.getVersion() );
				}
		);
	}
}
