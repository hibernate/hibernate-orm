/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.envers;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class AuditTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::envers-audited-insert-example[]
			Customer customer = new Customer();
			customer.setId( 1L );
			customer.setFirstName( "John" );
			customer.setLastName( "Doe" );

			entityManager.persist( customer );
			//end::envers-audited-insert-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::envers-audited-update-example[]
			Customer customer = entityManager.find( Customer.class, 1L );
			customer.setLastName( "Doe Jr." );
			//end::envers-audited-update-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::envers-audited-delete-example[]
			Customer customer = entityManager.getReference( Customer.class, 1L );
			entityManager.remove( customer );
			//end::envers-audited-delete-example[]
		} );
	}

	//tag::envers-audited-mapping-example[]
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

		//Getters and setters are omitted for brevity

	//end::envers-audited-mapping-example[]
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
	//tag::envers-audited-mapping-example[]
	}
	//end::envers-audited-mapping-example[]
}
