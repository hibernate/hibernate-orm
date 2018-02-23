/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11867")
public class UpdateTimeStampInheritanceTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class, AbstractPerson.class };
	}

	@Test
	public void updateParentClassProperty() {
		final String customerId = "1";
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( customerId );
			entityManager.persist( customer );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getCreatedAt(), is( customer.getModifiedAt() ) );
			customer.setName( "xyz" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getCreatedAt(), is( not( customer.getModifiedAt() ) ) );
		} );
	}

	@Test
	public void updateSubClassProperty() {
		final String customerId = "1";
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( customerId );
			entityManager.persist( customer );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getCreatedAt(), is( customer.getModifiedAt() ) );
			customer.setEmail( "xyz@" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getCreatedAt(), is( not( customer.getModifiedAt() ) ) );
		} );
	}

	@Entity(name = "person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class AbstractPerson {
		@Id
		@Column(name = "id")
		private String id;

		private String name;

		@CreationTimestamp
		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "created_at", updatable = false)
		private Date createdAt;

		@UpdateTimestamp
		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "modified_at")
		private Date modifiedAt;

		public void setId(String id) {
			this.id = id;
		}

		public Date getCreatedAt() {
			return createdAt;
		}

		public Date getModifiedAt() {
			return modifiedAt;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "customer")
	public static class Customer extends AbstractPerson {
		private String email;

		public void setEmail(String email) {
			this.email = email;
		}
	}
}
