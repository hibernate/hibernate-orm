/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fábio Takeo Ueno
 */
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class
		}
)
public class CascadeDetachTest {

	@Test
	public void detachTest(EntityManagerFactoryScope scope) {
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

			//tag::pc-cascade-detach-example[]
			Person person = entityManager.find( Person.class, 1L );
			assertThat( person.getPhones() ).hasSize( 1 );
			Phone phone = person.getPhones().get( 0 );

			assertThat( entityManager.contains( person ) ).isTrue();
			assertThat( entityManager.contains( phone ) ).isTrue();

			entityManager.detach( person );

			assertThat( entityManager.contains( person ) ).isFalse();
			assertThat( entityManager.contains( phone ) ).isFalse();

			//end::pc-cascade-detach-example[]
		} );
	}
}
