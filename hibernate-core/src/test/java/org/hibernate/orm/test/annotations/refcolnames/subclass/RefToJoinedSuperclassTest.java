/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.refcolnames.subclass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = { Animal.class, Cat.class, Toy.class })
public class RefToJoinedSuperclassTest {
	@Test
	public void test(SessionFactoryScope scope) {
		final Cat cat = new Cat();
		cat.setName( "cat" );
		final Toy toy = new Toy();
		cat.getToys().add( toy );
		scope.inTransaction( session -> {
			session.persist( cat );
			session.persist( toy );
		} );
		scope.inSession( session -> {
			Cat result = session.createQuery( "from Cat", Cat.class ).getSingleResult();
			assertEquals( 1, result.getToys().size() );
			assertEquals( toy.getId(), result.getToys().get( 0 ).getId() );
		} );
	}
}
