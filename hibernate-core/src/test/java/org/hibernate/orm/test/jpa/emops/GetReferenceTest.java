/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.emops;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */

@Jpa(annotatedClasses = {
		Competitor.class,
		Race.class,
		Mail.class
})
public class GetReferenceTest {
	@Test
	public void testWrongIdType(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getReference( Competitor.class, "30" );
						fail("Expected IllegalArgumentException");
					}
					catch (IllegalArgumentException e) {
						//success
					}
					catch ( Exception e ) {
						fail("Wrong exception: " + e );
					}

					try {
						entityManager.getReference( Mail.class, 1 );
						fail("Expected IllegalArgumentException");
					}
					catch (IllegalArgumentException e) {
						//success
					}
					catch ( Exception e ) {
						fail("Wrong exception: " + e );
					}
				}
		);
	}
}
