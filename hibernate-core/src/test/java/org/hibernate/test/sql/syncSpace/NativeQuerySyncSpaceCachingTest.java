/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.syncSpace;

import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests of how sync-spaces for a native query affect caching
 *
 * @author Samuel Fung
 * @author Steve Ebersole
 */
public class NativeQuerySyncSpaceCachingTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Customer.class, Address.class };
	}

	@Before
	public void before() {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		Customer customer = new Customer( 1, "Samuel" );
		session.saveOrUpdate( customer );
		session.getTransaction().commit();
		session.close();
	}

	@After
	public void after() {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		session.createQuery( "delete Customer" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testSelectAnotherEntityWithNoSyncSpaces() {
		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );

		Session session = openSession();
		session.createSQLQuery( "select * from Address" ).list();
		session.close();

		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );
	}

	@Test
	public void testUpdateAnotherEntityWithNoSyncSpaces() {
		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );

		Session session = openSession();
		session.beginTransaction();
		session.createSQLQuery( "update Address set id = id" ).executeUpdate();
		session.getTransaction().commit();
		session.close();

		// NOTE false here because executeUpdate is different than selects
		assertFalse( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );
	}

	@Test
	public void testUpdateAnotherEntityWithSyncSpaces() {
		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );

		Session session = openSession();
		session.beginTransaction();
		session.createSQLQuery( "update Address set id = id" ).addSynchronizedEntityClass( Address.class ).executeUpdate();
		session.getTransaction().commit();
		session.close();

		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );
	}

	@Test
	public void testSelectCachedEntityWithNoSyncSpaces() {
		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );

		Session session = openSession();
		session.createSQLQuery( "select * from Customer" ).list();
		session.close();

		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );
	}

	@Test
	public void testUpdateCachedEntityWithNoSyncSpaces() {
		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );

		Session session = openSession();
		session.beginTransaction();
		session.createSQLQuery( "update Customer set id = id" ).executeUpdate();
		session.getTransaction().commit();
		session.close();

		// NOTE false here because executeUpdate is different than selects
		assertFalse( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );
	}

	@Test
	public void testUpdateCachedEntityWithSyncSpaces() {
		assertTrue( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );

		Session session = openSession();
		session.beginTransaction();
		session.createSQLQuery( "update Customer set id = id" ).addSynchronizedEntityClass( Customer.class ).executeUpdate();
		session.getTransaction().commit();
		session.close();

		assertFalse( sessionFactory().getCache().containsEntity( Customer.class, 1 ) );
	}

	@Entity( name = "Customer" )
	@Table(name="Customer")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class Customer {
		@Id
		private int id;

		private String name;

		public Customer() {
		}

		public Customer(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "Address" )
	@Table(name="Address")
	public static class Address {
		@Id
		private int id;
		private String text;

		public Address() {
		}

		public Address(int id, String text) {
			this.id = id;
			this.text = text;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
