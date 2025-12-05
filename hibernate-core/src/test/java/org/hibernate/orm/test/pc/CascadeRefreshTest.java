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
public class CascadeRefreshTest {

	@Test
	public void refreshTest(EntityManagerFactoryScope scope) {
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

			//tag::pc-cascade-refresh-example[]
			Person person = entityManager.find( Person.class, 1L );
			Phone phone = person.getPhones().get( 0 );

			person.setName( "John Doe Jr." );
			phone.setNumber( "987-654-3210" );

			entityManager.refresh( person );

			assertThat( person.getName() ).isEqualTo( "John Doe" );
			assertThat( phone.getNumber() ).isEqualTo( "123-456-7890" );
			//end::pc-cascade-refresh-example[]
		} );
	}
}
