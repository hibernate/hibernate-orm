/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.net.URL;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {TypeCategoryTest.Contact.class} )
public class TypeCategoryTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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
	public static class Name {

		private String firstName;

		private String middleName;

		private String lastName;

		// getters and setters omitted
	}
	//end::mapping-types-basic-example[]
}
