/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class QueueTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( QueueTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class,
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( 1L );
			person.getPhones().add( new Phone( 1L, "landline", "028-234-9876" ) );
			person.getPhones().add( new Phone( 2L, "mobile", "072-122-9876" ) );
			entityManager.persist( person );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::collections-custom-collection-example[]
			Person person = entityManager.find( Person.class, 1L );
			Queue<Phone> phones = person.getPhones();
			Phone head = phones.peek();
			assertSame(head, phones.poll());
			assertEquals( 1, phones.size() );
			//end::collections-custom-collection-example[]
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			person.getPhones().clear();
		} );
	}

	//tag::collections-custom-collection-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@CollectionType( type = "org.hibernate.userguide.collections.type.QueueType")
		private Collection<Phone> phones = new LinkedList<>();

		//Getters and setters are omitted for brevity

	//end::collections-custom-collection-mapping-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public Queue<Phone> getPhones() {
			return (Queue<Phone>) phones;
		}
	//tag::collections-custom-collection-mapping-example[]
	}

	@Entity(name = "Phone")
	public static class Phone implements Comparable<Phone> {

		@Id
		private Long id;

		private String type;

		@NaturalId
		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::collections-custom-collection-mapping-example[]

		public Phone() {
		}

		public Phone(Long id, String type, String number) {
			this.id = id;
			this.type = type;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getNumber() {
			return number;
		}

		@Override
		public int compareTo(Phone o) {
			return number.compareTo( o.getNumber() );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Phone phone = (Phone) o;
			return Objects.equals( number, phone.number );
		}

		@Override
		public int hashCode() {
			return Objects.hash( number );
		}
	//tag::collections-custom-collection-mapping-example[]
	}
	//end::collections-custom-collection-mapping-example[]
}
