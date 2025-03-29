/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionMapping;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		CustomRevisionEntityTest.Customer.class,
		CustomRevisionEntityTest.CustomRevisionEntity.class
})
public class CustomRevisionEntityTest {
	@Test
	public void test(EntityManagerFactoryScope scope) {
		//tag::envers-revisionlog-RevisionEntity-persist-example[]
		CurrentUser.INSTANCE.logIn("Vlad Mihalcea");

		scope.inTransaction( entityManager -> {
			Customer customer = new Customer();
			customer.setId(1L);
			customer.setFirstName("John");
			customer.setLastName("Doe");

			entityManager.persist(customer);
		});

		CurrentUser.INSTANCE.logOut();
		//end::envers-revisionlog-RevisionEntity-persist-example[]
	}

	@Audited
	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "created_on")
		@CreationTimestamp
		private Date createdOn;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}
	}

	//tag::envers-revisionlog-CurrentUser-example[]
	public static class CurrentUser {

		public static final CurrentUser INSTANCE = new CurrentUser();

		private static final ThreadLocal<String> storage = new ThreadLocal<>();

		public void logIn(String user) {
			storage.set(user);
		}

		public void logOut() {
			storage.remove();
		}

		public String get() {
			return storage.get();
		}
	}
	//end::envers-revisionlog-CurrentUser-example[]

	//tag::envers-revisionlog-RevisionEntity-example[]
	@Entity(name = "CustomRevisionEntity")
	@Table(name = "CUSTOM_REV_INFO")
	@RevisionEntity(CustomRevisionEntityListener.class)
	public static class CustomRevisionEntity extends RevisionMapping {

		private String username;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}
	}
	//end::envers-revisionlog-RevisionEntity-example[]

	//tag::envers-revisionlog-RevisionListener-example[]
	public static class CustomRevisionEntityListener implements RevisionListener {

		public void newRevision(Object revisionEntity) {
			CustomRevisionEntity customRevisionEntity =
				(CustomRevisionEntity) revisionEntity;

			customRevisionEntity.setUsername(
				CurrentUser.INSTANCE.get()
			);
		}
	}
	//end::envers-revisionlog-RevisionListener-example[]
}
