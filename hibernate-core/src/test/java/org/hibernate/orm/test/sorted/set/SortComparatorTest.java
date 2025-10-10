/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sorted.set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.SortComparator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;


@DomainModel(
		annotatedClasses = {
				SortComparatorTest.Owner.class, SortComparatorTest.Cat.class
		}
)
@SessionFactory
public class SortComparatorTest {

	@Test
	public void testSortComparator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Owner owner = new Owner();
					Cat cat1 = new Cat();
					Cat cat2 = new Cat();
					cat1.owner = owner;
					cat1.name = "B";
					cat1.nickname = "B";
					cat2.owner = owner;
					cat2.name = "a";
					cat2.nickname = "a";
					owner.cats.add( cat1 );
					owner.cats.add( cat2 );
					session.persist( owner );

					session.getTransaction().commit();
					session.clear();

					session.beginTransaction();

					owner = session.get( Owner.class, owner.id );

					assertThat( owner.cats ).isNotNull();
					assertThat( owner.cats.size() ).isEqualTo( 2 );
					assertThat( owner.cats.first().nickname ).isEqualTo( "a" );
					assertThat( owner.cats.last().nickname ).isEqualTo( "B" );
				}
		);
	}

	@Entity(name = "Owner")
	@Table(name = "Owner")
	static class Owner {

		@Id
		@GeneratedValue
		private long id;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
		@SortComparator(CatNicknameComparator.class)
		private SortedSet<Cat> cats = new TreeSet<>();
	}

	@Entity(name = "Cat")
	@Table(name = "Cat")
	static class Cat implements Comparable<Cat> {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne
		private Owner owner;

		private String name;

		private String nickname;

		@Override
		public int compareTo(Cat other) {
			return this.name.compareTo( other.name );
		}

	}

	public static class CatNicknameComparator implements Comparator<Cat> {

		@Override
		public int compare(Cat cat1, Cat cat2) {
			return String.CASE_INSENSITIVE_ORDER.compare( cat1.nickname, cat2.nickname );
		}
	}
}
