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

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = Antenna.class
)
@SessionFactory
public class GeneratedTest {

	@Test
	public void testGenerated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Antenna antenna = new Antenna();
					antenna.id = new Integer( 1 );
					session.persist( antenna );
					assertNull( antenna.latitude );
					assertNull( antenna.longitude );
				}
		);
	}
}
