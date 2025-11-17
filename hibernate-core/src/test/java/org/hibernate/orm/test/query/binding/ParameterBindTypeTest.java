/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.binding;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = Person.class)
class ParameterBindTypeTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Address address = new Address( "Diagonal", "Barcelona", "CAT" );
			Person gavin = new Person( "Gavin", address );
			session.persist( gavin );
			assertEquals(1, session.createQuery( "from Person where address = :address", Person.class )
					.setParameter( "address", address, Address_.class_)
					.getResultList().size() );
			assertEquals(1, session.createQuery( "from Person where address = :address", Person.class )
					.setParameter( "address", address, Person_.address.getType())
					.getResultList().size() );
			assertEquals(1, session.createQuery( "from Person where address.street = :street", Person.class )
					.setParameter( "street", "Diagonal", Address_.street.getType())
					.getResultList().size() );
			assertEquals(1, session.createQuery( "from Person p where p = :person", Person.class )
					.setParameter( "person", new Person( "Gavin", null ), Person_.class_)
					.getResultList().size() );
		} );
	}

}
