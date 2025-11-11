/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.TrackingModifiedEntitiesRevisionMapping;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;


/**
 * @author Vlad Mihalcea
 */
public class EntityTypeChangeAuditDefaultTrackingTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Customer.class,
				CustomTrackingRevisionEntity.class
		};
	}

	@Test
	public void testLifecycle() {
		try (final EntityManagerFactory testEmf = produceEntityManagerFactory()) {

			TransactionUtil.doInJPA( () -> testEmf, entityManager -> {
				Customer customer = new Customer();
				customer.setId( 1L );
				customer.setFirstName( "John" );
				customer.setLastName( "Doe" );

				entityManager.persist( customer );
			} );

			try (EntityManagerFactory entityManagerFactory = buildEntityManagerFactory()) {
				entityManagerFactory.runInTransaction( entityManager -> {
					ApplicationCustomer customer = new ApplicationCustomer();
					customer.setId( 2L );
					customer.setFirstName( "John" );
					customer.setLastName( "Doe Jr." );

					entityManager.persist( customer );
					entityManager.getTransaction().commit();
				} );
			}
		}
	}

	private EntityManagerFactory buildEntityManagerFactory() {
		Map<Object, Object> settings = buildSettings();
		settings.put(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( ApplicationCustomer.class, CustomTrackingRevisionEntity.class )
		);
		settings.put( AvailableSettings.HBM2DDL_AUTO, "update" );
		return Bootstrap.getEntityManagerFactoryBuilder(
						new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
						settings )
				.build();
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

	@Audited
	@Entity(name = "Customer")
	public static class ApplicationCustomer {

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

	//tag::envers-tracking-modified-entities-revchanges-example[]
	@Entity(name = "CustomTrackingRevisionEntity")
	@Table(name = "TRACKING_REV_INFO")
	@RevisionEntity
	public static class CustomTrackingRevisionEntity
			extends TrackingModifiedEntitiesRevisionMapping {

	}
	//end::envers-tracking-modified-entities-revchanges-example[]
}
