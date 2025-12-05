/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sorted.map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.SortComparator;
import org.hibernate.orm.test.sorted.StringCaseInsensitiveComparator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;


@DomainModel(
		annotatedClasses = {
			SortComparatorTest.Owner.class,
				SortComparatorTest.Cat.class
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
					owner.cats.put( cat1.nickname, cat1 );
					owner.cats.put( cat2.nickname, cat2 );
					session.persist( owner );

					session.getTransaction().commit();
					session.clear();

					session.beginTransaction();

					owner = session.get( Owner.class, owner.id );
					assertThat( owner.cats ).isNotNull();
					assertThat( owner.cats.size()).isEqualTo(2);

					Iterator<Map.Entry<String, Cat>> entryIterator = owner.cats.entrySet().iterator();
					Map.Entry<String, Cat> firstEntry = entryIterator.next();
					Map.Entry<String, Cat> secondEntry = entryIterator.next();

					assertThat( firstEntry.getKey()).isEqualTo("a");
					assertThat( firstEntry.getValue().nickname).isEqualTo("a");
					assertThat( secondEntry.getKey()).isEqualTo("B");
					assertThat( secondEntry.getValue().nickname).isEqualTo("B");
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
		@MapKey(name = "nickname")
		@SortComparator( StringCaseInsensitiveComparator.class )
		private SortedMap<String, Cat> cats = new TreeMap<>();
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

}
