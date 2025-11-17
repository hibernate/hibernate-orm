/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionMapping;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Vlad Mihalcea
 */
public class EntityTypeChangeAuditTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Customer.class,
				CustomTrackingRevisionEntity.class
		};
	}

	@Test
	public void test() {
		try (final EntityManagerFactory testEmf = produceEntityManagerFactory()) {

			testEmf.runInTransaction( entityManager -> {
				Customer customer = new Customer();
				customer.setId( 1L );
				customer.setFirstName( "John" );
				customer.setLastName( "Doe" );

				entityManager.persist( customer );
			} );

			testEmf.runInTransaction( entityManager -> {
				//tag::envers-tracking-modified-entities-queries-example1[]
				assertThat(
						AuditReaderFactory
								.get( entityManager )
								.getCrossTypeRevisionChangesReader()
								.findEntityTypes( 1 )
								.iterator().next()
								.getFirst()
				).isEqualTo( "org.hibernate.orm.test.envers.EntityTypeChangeAuditTest$Customer" );
				//end::envers-tracking-modified-entities-queries-example1[]
			} );

			try (EntityManagerFactory entityManagerFactory = buildEntityManagerFactory()) {
				final EntityManagerFactory emf = entityManagerFactory;

				entityManagerFactory.runInTransaction( entityManager -> {
					ApplicationCustomer customer = entityManager.find( ApplicationCustomer.class, 1L );
					customer.setLastName( "Doe Jr." );
				} );

				entityManagerFactory.runInTransaction( entityManager -> {
					//tag::envers-tracking-modified-entities-queries-example2[]
					assertThat(
							AuditReaderFactory
									.get( entityManager )
									.getCrossTypeRevisionChangesReader()
									.findEntityTypes( 2 )
									.iterator().next()
									.getFirst()
					).isEqualTo( "org.hibernate.orm.test.envers.EntityTypeChangeAuditTest$ApplicationCustomer" );
					//end::envers-tracking-modified-entities-queries-example2[]
				} );
			}
		}
	}

	private EntityManagerFactory buildEntityManagerFactory() {
		Map<Object, Object> settings = buildSettings();
		settings.put(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList(
						ApplicationCustomer.class,
						CustomTrackingRevisionEntity.class
				)
		);
		settings.put( AvailableSettings.HBM2DDL_AUTO, "update" );
		return Bootstrap.getEntityManagerFactoryBuilder(
						new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
						settings )
				.build();
	}


	//tag::envers-tracking-modified-entities-revchanges-before-rename-example[]
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

		//Getters and setters are omitted for brevity
		//end::envers-tracking-modified-entities-revchanges-before-rename-example[]

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
		//tag::envers-tracking-modified-entities-revchanges-before-rename-example[]
	}
	//end::envers-tracking-modified-entities-revchanges-before-rename-example[]

	//tag::envers-tracking-modified-entities-revchanges-after-rename-example[]
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

		//Getters and setters are omitted for brevity
		//end::envers-tracking-modified-entities-revchanges-after-rename-example[]

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
		//tag::envers-tracking-modified-entities-revchanges-after-rename-example[]
	}
	//end::envers-tracking-modified-entities-revchanges-after-rename-example[]

	//tag::envers-tracking-modified-entities-revchanges-example[]
	@Entity(name = "CustomTrackingRevisionEntity")
	@Table(name = "TRACKING_REV_INFO")
	@RevisionEntity
	public static class CustomTrackingRevisionEntity extends RevisionMapping {

		@ElementCollection
		@JoinTable(
				name = "REVCHANGES",
				joinColumns = @JoinColumn(name = "REV")
		)
		@Column(name = "ENTITYNAME")
		@ModifiedEntityNames
		private Set<String> modifiedEntityNames = new HashSet<>();

		public Set<String> getModifiedEntityNames() {
			return modifiedEntityNames;
		}
	}
	//end::envers-tracking-modified-entities-revchanges-example[]
}
