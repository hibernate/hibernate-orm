/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreUpdate;
import javax.persistence.PreRemove;
import javax.persistence.PostRemove;
import javax.persistence.PostLoad;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Column;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.hibernate.testing.transaction.TransactionUtil2.fromTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddableCallbackTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class, User.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12326")
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = new Employee();
			employee.details = new EmployeeDetails();
			employee.id = 1;

			entityManager.persist( employee );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1 );

			assertEquals( "Vlad", employee.name );
			assertEquals( "Developer Advocate", employee.details.jobTitle );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13110")
	public void testNullEmbeddable() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = new Employee();
			employee.id = 1;

			entityManager.persist( employee );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1 );

			assertEquals( "Vlad", employee.name );
			assertNull( employee.details );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13829")
	public void testCollectionOfEmbeddable() {
		User user = fromTransaction( this.entityManagerFactory(), entityManager -> {
			final User u = new User();
			u.id = 1;
			u.userDetails = new UserDetails();
			u.contactAddresses = new ArrayList<>();
			u.contactAddresses.add( new ContactAddress() );
			u.contactAddresses.add( new ContactAddress() );

			entityManager.persist( u );

			u.contactAddresses.forEach( e -> assertEquals( 1, e.prePersist ) );
			return u;
		} );

		user.contactAddresses.forEach( e -> assertEquals( 1, e.postPersist ) );

		doInJPA( this::entityManagerFactory, entityManager -> {
			User entity = entityManager.find( User.class, 1 );

			assertEquals( "George", entity.name );
			assertEquals("London", entity.contactAddresses.get( 0 ).city );
			assertEquals("test@test.com", entity.userDetails.email );
			entity.contactAddresses.forEach( e -> assertEquals( 1, e.postLoad ) );
		} );

		user = fromTransaction( this.entityManagerFactory(), entityManager -> {
			User u = entityManager.find( User.class, 1 );
			u.name = "Nick";
			u.contactAddresses.get( 0 ).city = "Athens";

			entityManager.persist( u );

			return u;
		} );

		user.contactAddresses.forEach( e -> assertEquals( 1, e.preUpdate ) );
		user.contactAddresses.forEach( e -> assertEquals( 1, e.postUpdate ) );

		user = fromTransaction( this.entityManagerFactory(), entityManager -> {
			User u = entityManager.find( User.class, 1 );

			assertEquals( "Nick", u.name );
			assertEquals( "Athens", u.contactAddresses.get( 0 ).city );

			entityManager.remove( u );
			u.contactAddresses.forEach( e -> assertEquals( 1, e.preRemove ) );

			return u;
		} );

		user.contactAddresses.forEach( e -> assertEquals( 1, e.postRemove ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13829")
	public void testNullCollectionOfEmbeddable() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			User user = new User();
			user.id = 1;
			entityManager.persist( user);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			User user = entityManager.find( User.class, 1 );
			assertNull( user.userDetails );
			assertEquals( 0, user.contactAddresses.size() );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Integer id;

		private String name;

		private EmployeeDetails details;

		@PrePersist
		public void setUp() {
			name = "Vlad";
		}
	}

	@Embeddable
	public static class EmployeeDetails {

		private String jobTitle;

		@PrePersist
		public void setUp() {
			jobTitle = "Developer Advocate";
		}
	}

	@Entity(name = "User")
	public static class User {
		@Id
		private Integer id;

		private String name;

		private UserDetails userDetails;

		@ElementCollection
		@CollectionTable
		private List<ContactAddress> contactAddresses;

		@PrePersist
		public void setUp() {
			name = "George";
		}
	}

	@Embeddable
	public static class UserDetails {

		private String email;

		@PrePersist
		public void setUp() {
			email = "test@test.com";
		}
	}

	@Embeddable
	public static class ContactAddress {
		public int prePersist;
		public int postPersist;
		public int preUpdate;
		public int postUpdate;
		public int preRemove;
		public int postRemove;
		public int postLoad;

		@Column
		private String city;

		@PrePersist
		public void prePersist() {
			city = "London";
			prePersist++;
		}
		@PostPersist
		public void postPersist() {
			postPersist++;
		}
		@PreUpdate
		public void preUpdate() {
			preUpdate++;
		}
		@PostUpdate
		public void postUpdate() {
			postUpdate++;
		}
		@PreRemove
		public void preRemove() {
			preRemove++;
		}
		@PostRemove
		public void postRemove() {
			postRemove++;
		}
		@PostLoad
		public void postLoad() {
			postLoad++;
		}
	}
}
