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

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		MergeWithTransientNonCascadedAssociationTest.Person.class,
		MergeWithTransientNonCascadedAssociationTest.Address.class
})
@SessionFactory
public class MergeWithTransientNonCascadedAssociationTest {
	@Test
	public void testMergeWithTransientNonCascadedAssociation(SessionFactoryScope scope) {
		Person person = new Person();
		scope.inTransaction(
				session -> {
					session.persist( person );
				}
		);

		person.address = new Address();

		scope.inSession(
				session -> {
					session.getTransaction().begin();
					session.merge( person );
					try {
						session.flush();
						fail( "Expecting IllegalStateException" );
					}
					catch (IllegalStateException ise) {
						// expected...
						session.getTransaction().rollback();
					}
				}
		);

		scope.inTransaction(
				session -> {
					person.address = null;
					session.unwrap( Session.class ).lock( person, LockMode.NONE );
					session.unwrap( Session.class ).delete( person );
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
