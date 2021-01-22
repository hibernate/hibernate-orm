/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		MergeWithTransientNonCascadedAssociationTest.Person.class,
		MergeWithTransientNonCascadedAssociationTest.Address.class
})
public class MergeWithTransientNonCascadedAssociationTest {
	@Test
	public void testMergeWithTransientNonCascadedAssociation(EntityManagerFactoryScope scope) {
		Person person = new Person();
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( person );
				}
		);

		person.address = new Address();

		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					entityManager.merge( person );
					try {
						entityManager.flush();
						fail( "Expecting IllegalStateException" );
					}
					catch (IllegalStateException ise) {
						// expected...
						entityManager.getTransaction().rollback();
					}
				}
		);

		scope.inTransaction(
				entityManager -> {
					person.address = null;
					entityManager.unwrap( Session.class ).lock( person, LockMode.NONE );
					entityManager.unwrap( Session.class ).delete( person );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		private Integer id;
		@ManyToOne
		private Address address;

		public Person() {
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@GeneratedValue(generator = "increment_1")
		@GenericGenerator(name = "increment_1", strategy = "increment")
		private Integer id;
	}
}
