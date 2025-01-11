/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.embeddable.discriminatorvalues;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				Animal.class,
				Cat.class,
				Dog.class,
				Fish.class,
				Mammal.class,
				Owner.class
		}
)
@SessionFactory
public class EmbeddableInheritanceTest {

	@Test
	void testCatByMother(SessionFactoryScope scope) {
		final List<Owner> owners = scope.fromSession(
				session ->
						session.createQuery( "select o from Owner o where treat(o.pet as Cat).mother = :mother",
										Owner.class )
								.setParameter( "mother", "Chloe" )
								.getResultList() );
		assertNotNull( owners );
	}

	@Test
	void testCatMothers(SessionFactoryScope scope) {
		final List<String> owners = scope.fromSession(
				session ->
						session.createNamedQuery( "catMothers", String.class )
								.getResultList() );
		assertNotNull( owners );
	}
}
