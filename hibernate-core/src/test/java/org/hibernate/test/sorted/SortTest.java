/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.sorted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 * @author Brett Meyer
 */
public class SortTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected String[] getMappings() {
		return new String[] { "sorted/Search.hbm.xml" };
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Owner.class, Cat.class };
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrderBy() {
		Search s = new Search("Hibernate");
		s.getSearchResults().add("jboss.com");
		s.getSearchResults().add("hibernate.org");
		s.getSearchResults().add("HiA");
		
		Session sess = openSession();
		Transaction tx = sess.beginTransaction();
		sess.persist(s);
		sess.flush();
		
		sess.clear();
		s = (Search) sess.createCriteria(Search.class).uniqueResult();
		assertFalse( Hibernate.isInitialized( s.getSearchResults() ) );
		Iterator iter = s.getSearchResults().iterator();
		assertEquals( iter.next(), "HiA" );
		assertEquals( iter.next(), "hibernate.org" );
		assertEquals( iter.next(), "jboss.com" );
		assertFalse( iter.hasNext() );
		
		sess.clear();
		s = (Search) sess.createCriteria(Search.class)
				.setFetchMode("searchResults", FetchMode.JOIN)
				.uniqueResult();
		assertTrue( Hibernate.isInitialized( s.getSearchResults() ) );
		iter = s.getSearchResults().iterator();
		assertEquals( iter.next(), "HiA" );
		assertEquals( iter.next(), "hibernate.org" );
		assertEquals( iter.next(), "jboss.com" );
		assertFalse( iter.hasNext() );
		
		sess.clear();
		s = (Search) sess.createQuery("from Search s left join fetch s.searchResults")
				.uniqueResult();
		assertTrue( Hibernate.isInitialized( s.getSearchResults() ) );
		iter = s.getSearchResults().iterator();
		assertEquals( iter.next(), "HiA" );
		assertEquals( iter.next(), "hibernate.org" );
		assertEquals( iter.next(), "jboss.com" );
		assertFalse( iter.hasNext() );
		
		sess.delete(s);
		tx.commit();
		sess.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8827")
	public void testSortNatural() {
		Session s = openSession();
		s.beginTransaction();
		
		Owner owner = new Owner();
		Cat cat1 = new Cat();
		Cat cat2 = new Cat();
		cat1.owner = owner;
		cat1.name = "B";
		cat2.owner = owner;
		cat2.name = "A";
		owner.cats.add( cat1 );
		owner.cats.add( cat2 );
		s.persist( owner );
		
		s.getTransaction().commit();
		s.clear();
		
		s.beginTransaction();
		
		owner = (Owner) s.get( Owner.class, owner.id );
		assertNotNull(owner.cats);
		assertEquals(owner.cats.size(), 2);
		assertEquals(owner.cats.first().name, "A");
		assertEquals(owner.cats.last().name, "B");
		
		s.getTransaction().commit();
		s.close();
	}
	
	@Entity
	private static class Owner {

		@Id
		@GeneratedValue
		private long id;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
		@SortNatural
		private SortedSet<Cat> cats = new TreeSet<Cat>();
	}
	
	@Entity
	private static class Cat implements Comparable<Cat> {

		@Id
		@GeneratedValue
		private long id;

		@ManyToOne
		private Owner owner;
		
		private String name;

		@Override
		public int compareTo(Cat cat) {
			return this.name.compareTo( cat.name );
		}
	}

}

