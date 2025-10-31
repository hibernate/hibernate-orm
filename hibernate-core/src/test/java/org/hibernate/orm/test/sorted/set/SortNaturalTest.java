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
import org.hibernate.annotations.SortNatural;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				SortNaturalTest.Owner.class,
				SortNaturalTest.Cat.class
		}
)
@SessionFactory
public class SortNaturalTest {

	@Test
	@JiraKey(value = "HHH-8827")
	public void testSortNatural(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Owner owner = new Owner();
					Cat cat1 = new Cat();
					Cat cat2 = new Cat();
					cat1.owner = owner;
					cat1.name = "B";
					cat2.owner = owner;
					cat2.name = "A";
					owner.cats.add( cat1 );
					owner.cats.add( cat2 );
					session.persist( owner );

					session.getTransaction().commit();
					session.clear();

					session.beginTransaction();

					owner = session.get( Owner.class, owner.id );
					assertThat( owner.cats ).isNotNull();
					assertThat( owner.cats.size() ).isEqualTo( 2 );
					assertThat( owner.cats.first().name ).isEqualTo( "A" );
					assertThat( owner.cats.last().name ).isEqualTo( "B" );
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
		@SortNatural
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

		@Override
		public int compareTo(Cat other) {
			return this.name.compareTo( other.name );
		}
	}
}
