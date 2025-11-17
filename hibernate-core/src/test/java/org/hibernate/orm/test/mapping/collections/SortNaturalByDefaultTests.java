/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hamcrest.collection.IsIterableContainingInOrder;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests Hibernate-specific feature that {@link org.hibernate.annotations.SortNatural @SortNatural}
 * will take effect implicitly when no <i>sort</i> or <i>order</i> related annotations exist, including:
 * <ul>
 *     <li>{@link org.hibernate.annotations.SortNatural @SortNatural}</li>
 *     <li>{@link org.hibernate.annotations.SortComparator @SortComparator}</li>
 *     <li>{@link jakarta.persistence.OrderBy @OrderBy(from JPA)}</li>
 * </ul>
 *
 * @author Nathan Xu
 */
@ServiceRegistry
@DomainModel(
	annotatedClasses = {
		SortNaturalByDefaultTests.Person.class,
		SortNaturalByDefaultTests.Phone.class
	}
)
@SessionFactory
@JiraKey( value = "HHH-13877" )
public class SortNaturalByDefaultTests {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				final Person person = session.createQuery( "select p from Person p", Person.class ).getSingleResult();
				final SortedSet<Phone> phones = person.getPhones();
				assertThat( phones, IsIterableContainingInOrder.contains(
						new Phone( "123-456-789" ),
						new Phone( "234-567-891" ),
						new Phone( "345-678-912" ),
						new Phone( "456-789-123" ),
						new Phone( "567-891-234" ),
						new Phone( "678-912-345" ),
						new Phone( "789-123-456" ),
						new Phone( "891-234-567" ),
						new Phone( "912-345-678" ) )
				);
			}
		);
	}

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				final Person person = new Person();
				final SortedSet<Phone> phones = new TreeSet<>(
					Arrays.asList(
						new Phone( "678-912-345" ),
						new Phone( "234-567-891" ),
						new Phone( "567-891-234" ),
						new Phone( "456-789-123" ),
						new Phone( "123-456-789" ),
						new Phone( "912-345-678" ),
						new Phone( "789-123-456" ),
						new Phone( "345-678-912" ),
						new Phone( "891-234-567" )
					)
				);
				person.setPhones( phones );
				session.persist( person );
			}
		);
	}

	@AfterEach
	void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		private SortedSet<Phone> phones = new TreeSet<>(); // no '@SortNatural', '@SortComparator' or '@OrderBy' annotation present

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SortedSet<Phone> getPhones() {
			return phones;
		}

		public void setPhones(SortedSet<Phone> phones) {
			this.phones = phones;
		}
	}

	@Entity(name = "Phone")
	public static class Phone implements Comparable<Phone> {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "phone_number")
		private String number;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
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
}
