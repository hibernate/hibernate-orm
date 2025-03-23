/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sorted.map;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.SortComparator;
import org.hibernate.orm.test.sorted.StringCaseInsensitiveComparator;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings( "unused" )
public class SortComparatorTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Owner.class, Cat.class };
	}

	@Test
	public void testSortComparator() {
		Session s = openSession();
		s.beginTransaction();

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
		s.persist( owner );

		s.getTransaction().commit();
		s.clear();

		s.beginTransaction();

		owner = s.get( Owner.class, owner.id );
		assertNotNull( owner.cats );
		assertEquals( 2, owner.cats.size() );

		Iterator<Map.Entry<String, Cat>> entryIterator = owner.cats.entrySet().iterator();
		Map.Entry<String, Cat> firstEntry = entryIterator.next();
		Map.Entry<String, Cat> secondEntry = entryIterator.next();

		assertEquals( "a", firstEntry.getKey() );
		assertEquals( "a", firstEntry.getValue().nickname );
		assertEquals( "B", secondEntry.getKey() );
		assertEquals( "B", secondEntry.getValue().nickname );

		s.getTransaction().commit();
		s.close();
	}

	@Entity(name = "Owner")
	@Table(name = "Owner")
	private static class Owner {

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
	private static class Cat implements Comparable<Cat> {

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
