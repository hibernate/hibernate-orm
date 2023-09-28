/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Christian Beikov
 */
public class CollectionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		// Make sure this stuff runs on a dedicated connection pool,
		// otherwise we might run into ORA-21700: object does not exist or is marked for delete
		// because the JDBC connection or database session caches something that should have been invalidated
		options.put( AvailableSettings.CONNECTION_PROVIDER, "" );
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person(1L);
			List<String> phones = new ArrayList<>();
			phones.add( "028-234-9876" );
			phones.add( "072-122-9876" );
			person.setPhones(phones);
			entityManager.persist(person);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			List<String> phones = new ArrayList<>();
			phones.add( "072-122-9876" );
			person.setPhones(phones);
		});
	}

	//tag::collections-as-basic-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private List<String> phones;

		//Getters and setters are omitted for brevity

	//end::collections-as-basic-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<String> getPhones() {
			return phones;
		}

		public void setPhones(List<String> phones) {
			this.phones = phones;
		}
	//tag::collections-as-basic-example[]
	}
	//end::collections-as-basic-example[]
}
