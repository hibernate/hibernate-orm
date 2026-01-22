/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.orm.test.query.criteria.SelectionSpecificationTest_.Person_;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = SelectionSpecificationTest.Person.class)
public class SelectionSpecificationTest {

	@Test
	public void testCountAndExists(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person();
			person.id = 1L;
			person.name = "Gavin";
			session.persist( person );
			person = new Person();
			person.id = 2L;
			person.name = "Steve";
			session.persist( person );
		} );

		scope.inTransaction( session -> {
			var spec = SelectionSpecification.create( Person.class );

			assertEquals( 2L,
					spec.createCountProjection().createQuery( session ).getSingleResult() );
			assertTrue( spec.createExistsProjection().createQuery( session ).getSingleResult() );

			spec.restrict( Restriction.equal( Person_.name, "Gavin" ) );

			assertEquals( 1L,
					spec.createCountProjection().createQuery( session ).getSingleResult() );
			assertTrue( spec.createExistsProjection().createQuery( session ).getSingleResult() );

			spec.restrict( Restriction.equal( Person_.name, "Emmanuel" ) );

			assertEquals( 0L,
					spec.createCountProjection().createQuery( session ).getSingleResult() );

			assertFalse( spec.createExistsProjection().createQuery( session ).getSingleResult() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;

		String name;
	}
}
