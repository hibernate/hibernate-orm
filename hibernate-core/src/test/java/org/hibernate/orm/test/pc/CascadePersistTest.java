/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Fábio Takeo Ueno
 */
public class CascadePersistTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class
		};
	}

	@Test
	public void persistTest() {
		doInJPA(this::entityManagerFactory, entityManager -> {
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
