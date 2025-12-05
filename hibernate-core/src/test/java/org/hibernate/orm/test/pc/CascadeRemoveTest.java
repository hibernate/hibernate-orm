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
public class CascadeRemoveTest {

	@Test
	public void removeTest(EntityManagerFactoryScope scope) {
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
			//tag::pc-cascade-remove-example[]
			Person person = entityManager.find( Person.class, 1L );

			entityManager.remove( person );
			//end::pc-cascade-remove-example[]
		} );
	}
}
