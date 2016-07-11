/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.net.URL;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

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
		doInJPA( this::entityManagerFactory, entityManager -> {
			Contact contact = new Contact( );
			contact.id = 1;
			entityManager.persist( contact );
		} );
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

		private String first;

		private String middle;

		private String last;

		// getters and setters omitted
	}
	//end::mapping-types-basic-example[]
}
