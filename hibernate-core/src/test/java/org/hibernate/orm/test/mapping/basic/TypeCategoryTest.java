/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.net.URL;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class TypeCategoryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Contact.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Contact contact = new Contact();
			contact.id = 1;
			entityManager.persist(contact);
		});
	}

	//tag::mapping-types-basic-example[]
	@Entity(name = "Contact")
	public static class Contact {

		@Id
		private Integer id;

		private Name name;

		private String notes;

		private URL website;

		private boolean starred;

		//Getters and setters are omitted for brevity
	}

	@Embeddable
	public class Name {

		private String firstName;

		private String middleName;

		private String lastName;

		// getters and setters omitted
	}
	//end::mapping-types-basic-example[]
}
