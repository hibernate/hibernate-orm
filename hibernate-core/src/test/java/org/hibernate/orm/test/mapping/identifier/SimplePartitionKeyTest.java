/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import org.hibernate.annotations.PartitionKey;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Christian Beikov
 */
public class SimplePartitionKeyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			User.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			User user = new User();
			user.setId( 1L );
			user.setFirstname( "John" );
			user.setLastname( "Doe" );
			user.setTenantKey( "tenant1" );

			entityManager.persist( user );
		});
		doInJPA( this::entityManagerFactory, entityManager -> {
			User user = entityManager.find( User.class, 1L );
			user.setLastname( "Cash" );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.remove( entityManager.find( User.class, 1L ) );
		} );
	}

	@Table(name = "user_tbl")
	//tag::partition-key-simple-basic-attribute-mapping-example[]
	@Entity(name = "User")
	public static class User {

		@Id
		private Long id;

		private String firstname;

		private String lastname;

		@PartitionKey
		private String tenantKey;

		//Getters and setters are omitted for brevity
	//end::partition-key-simple-basic-attribute-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String title) {
			this.firstname = title;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String author) {
			this.lastname = author;
		}

		public String getTenantKey() {
			return tenantKey;
		}

		public void setTenantKey(String tenantKey) {
			this.tenantKey = tenantKey;
		}
	//tag::partition-key-simple-basic-attribute-mapping-example[]
	}
	//end::partition-key-simple-basic-attribute-mapping-example[]
}
