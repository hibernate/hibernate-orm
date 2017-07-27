/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.envers;

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class CustomRevisionEntityTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class,
			CustomRevisionEntity.class
		};
	}

	@Test
	public void test() {
		//tag::envers-revisionlog-RevisionEntity-persist-example[]
		CurrentUser.INSTANCE.logIn( "Vlad Mihalcea" );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( 1L );
			customer.setFirstName( "John" );
			customer.setLastName( "Doe" );

			entityManager.persist( customer );
		} );

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

	//tag::envers-revisionlog-CurrentUser-example[]
	public static class CurrentUser {

		public static final CurrentUser INSTANCE = new CurrentUser();

		private static final ThreadLocal<String> storage = new ThreadLocal<>();

		public void logIn(String user) {
			storage.set( user );
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
	@RevisionEntity( CustomRevisionEntityListener.class )
	public static class CustomRevisionEntity extends DefaultRevisionEntity {

		private String username;

		public String getUsername() {
			return username;
		}

		public void setUsername( String username ) {
			this.username = username;
		}
	}
	//end::envers-revisionlog-RevisionEntity-example[]

	//tag::envers-revisionlog-RevisionListener-example[]
	public static class CustomRevisionEntityListener implements RevisionListener {

		public void newRevision( Object revisionEntity ) {
			CustomRevisionEntity customRevisionEntity =
				( CustomRevisionEntity ) revisionEntity;

			customRevisionEntity.setUsername(
				CurrentUser.INSTANCE.get()
			);
		}
	}
	//end::envers-revisionlog-RevisionListener-example[]
}
