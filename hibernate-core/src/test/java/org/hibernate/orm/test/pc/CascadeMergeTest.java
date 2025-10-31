/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Fábio Takeo Ueno
 */
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class
		}
)
public class CascadeMergeTest {

	@Test
	public void mergeTest(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );

			Phone phone = new Phone();
			phone.setId( 1L );
			phone.setNumber( "123-456-7890" );

			person.addPhone( phone );

			entityManager.persist( person );

		} );

		scope.inTransaction( entityManager -> {
			//tag::pc-cascade-merge-example[]
			Phone phone = entityManager.find( Phone.class, 1L );
			Person person = phone.getOwner();

			person.setName( "John Doe Jr." );
			phone.setNumber( "987-654-3210" );

			entityManager.clear();

			entityManager.merge( person );
			//end::pc-cascade-merge-example[]
		} );
	}
}
