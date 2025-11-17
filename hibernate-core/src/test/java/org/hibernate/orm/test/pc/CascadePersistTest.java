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
public class CascadePersistTest {

	@Test
	public void persistTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(  entityManager -> {
			//tag::pc-cascade-persist-example[]
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");

			Phone phone = new Phone();
			phone.setId(1L);
			phone.setNumber("123-456-7890");

			person.addPhone(phone);

			entityManager.persist(person);
			//end::pc-cascade-persist-example[]
		});
	}
}
