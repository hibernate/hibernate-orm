/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SortNatural;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalSortedSetTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( UnidirectionalSortedSetTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( 1L );
			person.getPhones().add( new Phone( 1L, "landline", "028-234-9876" ) );
			person.getPhones().add( new Phone( 2L, "mobile", "072-122-9876" ) );
			entityManager.persist( person );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			Set<Phone> phones = person.getPhones();
			Assert.assertEquals( 2, phones.size() );
			phones.stream().forEach( phone -> log.infov( "Phone number %s", phone.getNumber() ) );
			phones.remove( phones.iterator().next() );
			Assert.assertEquals( 1, phones.size() );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			Set<Phone> phones = person.getPhones();
			Assert.assertEquals( 1, phones.size() );
		} );
	}

	//tag::collections-unidirectional-sorted-set-natural-comparator-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;
		@OneToMany(cascade = CascadeType.ALL)
		@SortNatural
		private SortedSet<Phone> phones = new TreeSet<>();

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public Set<Phone> getPhones() {
			return phones;
		}
	}

	@Entity(name = "Phone")
	public static class Phone implements Comparable<Phone> {

		@Id
		private Long id;

		private String type;

		@NaturalId
		@Column(name = "`number`")
		private String number;

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
	}
	//end::collections-unidirectional-sorted-set-natural-comparator-example[]
}
