/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11867")
public class UpdateTimeStampInheritanceTest extends BaseEntityManagerFunctionalTestCase {

	private final String customerId = "1";
	private static final long SLEEP_MILLIS = 250;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Customer.class,
			AbstractPerson.class,
			Author.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( customerId );
			entityManager.persist( customer );
		} );
	}

	@Test
	public void updateParentClassProperty() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertFalse( customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime() >= SLEEP_MILLIS );

			sleep( SLEEP_MILLIS );

			customer.setName( "xyz" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );

			assertTrue( customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime() >= SLEEP_MILLIS );
		} );
	}

	@Test
	public void updateSubClassProperty() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertFalse( customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime() >= SLEEP_MILLIS );

			sleep( SLEEP_MILLIS );

			customer.setEmail( "xyz@" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );

			assertTrue( customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime() >= SLEEP_MILLIS );
		} );
	}

	@Test
	public void updateDetachedEntity() {

		Customer customer = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.find( Customer.class, customerId );
		} );

		assertFalse( customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime() >= SLEEP_MILLIS );

		sleep( SLEEP_MILLIS );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).update( customer );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer _customer = entityManager.find( Customer.class, customerId );

			assertTrue( _customer.getModifiedAt().getTime() - _customer.getCreatedAt().getTime() >= SLEEP_MILLIS );
		} );
	}

	@Test
	public void updateSubClassCollection() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Author author = new Author();
			author.setId( "Vlad Mihalcea" );
			author.getBooks().add( "High-Performance Java Persistence, Part 1" );
			author.getBooks().add( "High-Performance Java Persistence, Part 2" );

			entityManager.persist( author );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Author author = entityManager.find( Author.class, "Vlad Mihalcea" );
			assertFalse( author.getModifiedAt().getTime() - author.getCreatedAt().getTime() >= SLEEP_MILLIS );

			sleep( SLEEP_MILLIS );

			author.setBooks( new ArrayList<>() );
			author.getBooks().add( "High-Performance Java Persistence, Part 3" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Author author = entityManager.find( Author.class, "Vlad Mihalcea" );

			assertTrue( author.getModifiedAt().getTime() - author.getCreatedAt().getTime() >= SLEEP_MILLIS );
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

		public void setCreatedAt(Date createdAt) {
			this.createdAt = createdAt;
		}

		public void setModifiedAt(Date modifiedAt) {
			this.modifiedAt = modifiedAt;
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

	@Entity
	@Table(name = "Author")
	public static class Author extends AbstractPerson {

		@ElementCollection
		private List<String> books = new ArrayList<>();

		public List<String> getBooks() {
			return books;
		}

		public void setBooks(List<String> books) {
			this.books = books;
		}
	}
}
