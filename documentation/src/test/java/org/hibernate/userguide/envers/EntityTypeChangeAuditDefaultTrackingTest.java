/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.envers;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class EntityTypeChangeAuditDefaultTrackingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class,
			CustomTrackingRevisionEntity.class
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( 1L );
			customer.setFirstName( "John" );
			customer.setLastName( "Doe" );

			entityManager.persist( customer );
		} );

		EntityManagerFactory entityManagerFactory = null;
		try {
			Map settings = buildSettings();
			settings.put(
				org.hibernate.jpa.AvailableSettings.LOADED_CLASSES,
				Arrays.asList(
					ApplicationCustomer.class,
					CustomTrackingRevisionEntity.class
				)
			);
			settings.put(
					AvailableSettings.HBM2DDL_AUTO,
					"update"
			);
			entityManagerFactory =  Bootstrap
			.getEntityManagerFactoryBuilder(
				new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
				settings )
			.build()
			.unwrap( SessionFactoryImplementor.class );

			final EntityManagerFactory emf = entityManagerFactory;

			doInJPA( () -> emf, entityManager -> {
				ApplicationCustomer customer = new ApplicationCustomer();
				customer.setId( 2L );
				customer.setFirstName( "John" );
				customer.setLastName( "Doe Jr." );

				entityManager.persist( customer );
			} );
		}
		finally {
			if ( entityManagerFactory != null ) {
				entityManagerFactory.close();
			}
		}
	}

	@Audited
	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal( TemporalType.TIMESTAMP )
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

		@Temporal( TemporalType.TIMESTAMP )
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
		extends DefaultTrackingModifiedEntitiesRevisionEntity {

	}
	//end::envers-tracking-modified-entities-revchanges-example[]
}
