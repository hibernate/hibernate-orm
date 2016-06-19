/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sorted;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 * @author Brett Meyer
 */
public class SortTest extends BaseNonConfigCoreFunctionalTestCase {
	
	@Override
	protected String[] getMappings() {
		return new String[] { "sorted/Search.hbm.xml" };
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Owner.class, Cat.class };
	}

	@Test
	public void testSortedSetDefinitionInHbmXml() {
		final PersistentClass entityMapping = metadata().getEntityBinding( Search.class.getName() );

		final Property sortedSetProperty = entityMapping.getProperty( "searchResults" );
		final Collection sortedSetMapping = assertTyping( Collection.class, sortedSetProperty.getValue()  );
		assertTrue( "SortedSet mapping not interpreted as sortable", sortedSetMapping.isSorted() );

		final Property sortedMapProperty = entityMapping.getProperty( "tokens" );
		final Collection sortedMapMapping = assertTyping( Collection.class, sortedMapProperty.getValue()  );
		assertTrue( "SortedMap mapping not interpreted as sortable", sortedMapMapping.isSorted() );
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
	@Table(name = "Owner")
	private static class Owner {

		@Id
		@GeneratedValue
		private long id;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
		@SortNatural
		private SortedSet<Cat> cats = new TreeSet<Cat>();
	}
	
	@Entity
	@Table(name = "Cat")
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

