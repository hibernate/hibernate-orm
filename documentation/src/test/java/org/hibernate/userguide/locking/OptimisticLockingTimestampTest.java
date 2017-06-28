/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.locking;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class OptimisticLockingTimestampTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		Person _person = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person(  );
			person.setName( "John Doe" );
			entityManager.persist( person );

			return person;
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, _person.getId() );
			person.setName( person.getName().toUpperCase() );
		} );
	}

	//tag::locking-optimistic-entity-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`name`")
		private String name;

		//tag::locking-optimistic-version-number-example[]
		@Version
		private Timestamp version;
		//end::locking-optimistic-version-number-example[]

		//Getters and setters are omitted for brevity

		//end::locking-optimistic-entity-mapping-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Timestamp getVersion() {
			return version;
		}
	//tag::locking-optimistic-entity-mapping-example[]
	}
	//end::locking-optimistic-entity-mapping-example[]
}
