/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.query;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.Tuple;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Distributor;
import org.hibernate.jpa.test.Item;
import org.hibernate.jpa.test.Wallet;
import org.hibernate.stat.Statistics;

import junit.framework.Assert;

import org.hibernate.testing.TestForIssue;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class QueryTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Distributor.class,
				Wallet.class,
				Employee.class,
				Contractor.class
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7192" )
	public void testTypedManipulationQueryError() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			em.createQuery( "delete Item", Item.class );
			fail();
		}
		catch (IllegalArgumentException expected) {
		}

		try {
			em.createQuery( "update Item i set i.name = 'someName'", Item.class );
			fail();
		}
		catch (IllegalArgumentException expected) {
		}
	}
	
	@Test
	public void testPagedQuery() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		em.persist( item );
		item = new Item( "Computer", "Apple II" );
		em.persist( item );
		Query q = em.createQuery( "select i from " + Item.class.getName() + " i where i.name like :itemName" );
		q.setParameter( "itemName", "%" );
		q.setMaxResults( 1 );
		q.getSingleResult();
		q = em.createQuery( "select i from Item i where i.name like :itemName" );
		q.setParameter( "itemName", "%" );
		q.setFirstResult( 1 );
		q.setMaxResults( 1 );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testAggregationReturnType() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		em.persist( item );
		item = new Item( "Computer", "Apple II" );
		em.persist( item );
		Query q = em.createQuery( "select count(i) from Item i where i.name like :itemName" );
		q.setParameter( "itemName", "%" );
		assertTrue( q.getSingleResult() instanceof Long );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testTypeExpression() throws Exception {
		final EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		final Employee employee = new Employee( "Lukasz", 100.0 );
		em.persist( employee );
		final Contractor contractor = new Contractor( "Kinga", 100.0, "Microsoft" );
		em.persist( contractor );
		final Query q = em.createQuery( "SELECT e FROM Employee e where TYPE(e) <> Contractor" );
		final List result = q.getResultList();
		assertNotNull( result );
		assertEquals( Arrays.asList( employee ), result );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH_7407" )
	public void testMultipleParameterLists() throws Exception {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		em.persist( item2 );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		List<String> names = Arrays.asList( item.getName() );
		Query q = em.createQuery( "select item from Item item where item.name in :names or item.name in :names2" );
		q.setParameter( "names", names );
		q.setParameter( "names2", names );
		List result = q.getResultList();
		assertNotNull( result );
		assertEquals( 1, result.size() );

		List<String> descrs = Arrays.asList( item.getDescr() );
		q = em.createQuery( "select item from Item item where item.name in :names and ( item.descr is null or item.descr in :descrs )" );
		q.setParameter( "names", names );
		q.setParameter( "descrs", descrs );
		result = q.getResultList();
		assertNotNull( result );
		assertEquals( 1, result.size() );

		em.getTransaction().begin();
		em.remove( em.getReference( Item.class, item.getName() ) );
		em.remove( em.getReference( Item.class, item2.getName() ) );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testJpaPositionalParameters() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Query query = em.createQuery( "from Item item where item.name =?1 or item.descr = ?1" );
		Parameter p1 = query.getParameter( 1 );
		Assert.assertNotNull( p1 );
		Assert.assertNotNull( p1.getPosition() );
		Assert.assertNull( p1.getName() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testParameterList() throws Exception {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		em.persist( item2 );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		Query q = em.createQuery( "select item from Item item where item.name in :names" );
		//test hint in value and string
		q.setHint( "org.hibernate.fetchSize", 10 );
		q.setHint( "org.hibernate.fetchSize", "10" );
		List params = new ArrayList();
		params.add( item.getName() );
		q.setParameter( "names", params );
		List result = q.getResultList();
		assertNotNull( result );
		assertEquals( 1, result.size() );

		q = em.createQuery( "select item from Item item where item.name in :names" );
		//test hint in value and string
		q.setHint( "org.hibernate.fetchSize", 10 );
		q.setHint( "org.hibernate.fetchSize", "10" );
		params.add( item2.getName() );
		q.setParameter( "names", params );
		result = q.getResultList();
		assertNotNull( result );
		assertEquals( 2, result.size() );

		q = em.createQuery( "select item from Item item where item.name in ?1" );
		params = new ArrayList();
		params.add( item.getName() );
		params.add( item2.getName() );
		// deprecated usage of positional parameter by String
		q.setParameter( "1", params );
		result = q.getResultList();
		assertNotNull( result );
		assertEquals( 2, result.size() );
		em.remove( result.get( 0 ) );
		em.remove( result.get( 1 ) );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testParameterListInExistingParens() throws Exception {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		em.persist( item2 );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		Query q = em.createQuery( "select item from Item item where item.name in (:names)" );
		//test hint in value and string
		q.setHint( "org.hibernate.fetchSize", 10 );
		q.setHint( "org.hibernate.fetchSize", "10" );
		List params = new ArrayList();
		params.add( item.getName() );
		params.add( item2.getName() );
		q.setParameter( "names", params );
		List result = q.getResultList();
		assertNotNull( result );
		assertEquals( 2, result.size() );

		q = em.createQuery( "select item from Item item where item.name in ( \n :names \n)\n" );
		//test hint in value and string
		q.setHint( "org.hibernate.fetchSize", 10 );
		q.setHint( "org.hibernate.fetchSize", "10" );
		params = new ArrayList();
		params.add( item.getName() );
		params.add( item2.getName() );
		q.setParameter( "names", params );
		result = q.getResultList();
		assertNotNull( result );
		assertEquals( 2, result.size() );

		q = em.createQuery( "select item from Item item where item.name in ( ?1 )" );
		params = new ArrayList();
		params.add( item.getName() );
		params.add( item2.getName() );
		// deprecated usage of positional parameter by String
		q.setParameter( "1", params );
		result = q.getResultList();
		assertNotNull( result );
		assertEquals( 2, result.size() );
		em.remove( result.get( 0 ) );
		em.remove( result.get( 1 ) );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testEscapeCharacter() throws Exception {
		final Item item = new Item( "Mouse", "Micro_oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		em.persist( item2 );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		Query q = em.createQuery( "select item from Item item where item.descr like 'Microk_oft mouse' escape 'k' " );
		List result = q.getResultList();
		assertNotNull( result );
		assertEquals( 1, result.size() );
		int deleted = em.createQuery( "delete from Item" ).executeUpdate();
		assertEquals( 2, deleted );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testNativeQueryByEntity() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		Statistics stats = em.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics();
		stats.clear();
		assertEquals( 0, stats.getFlushCount() );

		em.getTransaction().begin();
		item = (Item) em.createNativeQuery( "select * from Item", Item.class ).getSingleResult();
		assertEquals( 1, stats.getFlushCount() );
		assertNotNull( item );
		assertEquals( "Micro$oft mouse", item.getDescr() );
		em.remove( item );
		em.getTransaction().commit();

		em.close();

	}

	@Test
	public void testNativeQueryByResultSet() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		item = (Item) em.createNativeQuery( "select name as itemname, descr as itemdescription from Item", "getItem" )
				.getSingleResult();
		assertNotNull( item );
		assertEquals( "Micro$oft mouse", item.getDescr() );
		em.remove( item );
		em.getTransaction().commit();

		em.close();

	}

	@Test
	public void testExplicitPositionalParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Wallet w = new Wallet();
		w.setBrand( "Lacoste" );
		w.setModel( "Minimic" );
		w.setSerial( "0100202002" );
		em.persist( w );
		em.getTransaction().commit();
		em.getTransaction().begin();
		Query query = em.createQuery( "select w from " + Wallet.class.getName() + " w where w.brand in ?1" );
		List brands = new ArrayList();
		brands.add( "Lacoste" );
		query.setParameter( 1, brands );
		w = (Wallet) query.getSingleResult();
		assertNotNull( w );
		query = em.createQuery( "select w from " + Wallet.class.getName() + " w where w.marketEntrance = ?1" );
		query.setParameter( 1, new Date(), TemporalType.DATE );
		//assertNull( query.getSingleResult() );
		assertEquals( 0, query.getResultList().size() );
		em.remove( w );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTemporalTypeBinding() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Query query = em.createQuery( "select w from " + Wallet.class.getName() + " w where w.marketEntrance = :me" );
		Parameter parameter = query.getParameter( "me", Date.class );
		assertEquals( parameter.getParameterType(), Date.class );

		query.setParameter( "me", new Date() );
		query.setParameter( "me", new Date(), TemporalType.DATE );
		query.setParameter( "me", new GregorianCalendar(), TemporalType.DATE );

		em.getTransaction().commit();
		em.close();

	}

	@Test
	public void testPositionalParameterForms() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Wallet w = new Wallet();
		w.setBrand( "Lacoste" );
		w.setModel( "Minimic" );
		w.setSerial( "0100202002" );
		em.persist( w );
		em.getTransaction().commit();

		em.getTransaction().begin();
		// first using jpa-style positional parameter
		Query query = em.createQuery( "select w from Wallet w where w.brand = ?1" );
		query.setParameter( 1, "Lacoste" );
		w = (Wallet) query.getSingleResult();
		assertNotNull( w );

		// next using jpa-style positional parameter, but as a name (which is how Hibernate core treats these
		query = em.createQuery( "select w from Wallet w where w.brand = ?1" );
		// deprecated usage of positional parameter by String
		query.setParameter( "1", "Lacoste" );
		w = (Wallet) query.getSingleResult();
		assertNotNull( w );

		// finally using hql-style positional parameter
		query = em.createQuery( "select w from Wallet w where w.brand = ?" );
		query.setParameter( 1, "Lacoste" );
		w = (Wallet) query.getSingleResult();
		assertNotNull( w );

		em.remove( w );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testPositionalParameterWithUserError() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Wallet w = new Wallet();
		w.setBrand( "Lacoste" );
		w.setModel( "Minimic" );
		w.setSerial( "0100202002" );
		em.persist( w );
		em.flush();


		Query query = em.createQuery( "select w from Wallet w where w.brand = ?1 and w.model = ?3" );
		query.setParameter( 1, "Lacoste" );
		try {
			query.setParameter( 2, "Expensive" );
			fail( "Should fail due to a user error in parameters" );
		}
		catch ( IllegalArgumentException e ) {
			//success
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	public void testNativeQuestionMarkParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Wallet w = new Wallet();
		w.setBrand( "Lacoste" );
		w.setModel( "Minimic" );
		w.setSerial( "0100202002" );
		em.persist( w );
		em.getTransaction().commit();
		em.getTransaction().begin();
		Query query = em.createNativeQuery( "select * from Wallet w where w.brand = ?", Wallet.class );
		query.setParameter( 1, "Lacoste" );
		w = (Wallet) query.getSingleResult();
		assertNotNull( w );
		em.remove( w );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testNativeQueryWithPositionalParameter() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		Query query = em.createNativeQuery( "select * from Item where name = ?1", Item.class );
		query.setParameter( 1, "Mouse" );
		item = (Item) query.getSingleResult();
		assertNotNull( item );
		assertEquals( "Micro$oft mouse", item.getDescr() );
		query = em.createNativeQuery( "select * from Item where name = ?", Item.class );
		query.setParameter( 1, "Mouse" );
		item = (Item) query.getSingleResult();
		assertNotNull( item );
		assertEquals( "Micro$oft mouse", item.getDescr() );
		em.remove( item );
		em.getTransaction().commit();

		em.close();

	}

	@Test
	public void testDistinct() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Item" ).executeUpdate();
		em.createQuery( "delete Distributor" ).executeUpdate();
		Distributor d1 = new Distributor();
		d1.setName( "Fnac" );
		Distributor d2 = new Distributor();
		d2.setName( "Darty" );
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		item.getDistributors().add( d1 );
		item.getDistributors().add( d2 );
		em.persist( d1 );
		em.persist( d2 );
		em.persist( item );
		em.flush();
		em.clear();
		Query q = em.createQuery( "select distinct i from Item i left join fetch i.distributors" );
		item = (Item) q.getSingleResult()
				;
		//assertEquals( 1, distinctResult.size() );
		//item = (Item) distinctResult.get( 0 );
		assertTrue( Hibernate.isInitialized( item.getDistributors() ) );
		assertEquals( 2, item.getDistributors().size() );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testIsNull() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Distributor d1 = new Distributor();
		d1.setName( "Fnac" );
		Distributor d2 = new Distributor();
		d2.setName( "Darty" );
		Item item = new Item( "Mouse", null );
		Item item2 = new Item( "Mouse2", "dd" );
		item.getDistributors().add( d1 );
		item.getDistributors().add( d2 );
		em.persist( d1 );
		em.persist( d2 );
		em.persist( item );
		em.persist( item2 );
		em.flush();
		em.clear();
		Query q = em.createQuery(
				"select i from Item i where i.descr = :descr or (i.descr is null and cast(:descr as string) is null)"
		);
		//Query q = em.createQuery( "select i from Item i where (i.descr is null and :descr is null) or (i.descr = :descr");
		q.setParameter( "descr", "dd" );
		List result = q.getResultList();
		assertEquals( 1, result.size() );
		q.setParameter( "descr", null );
		result = q.getResultList();
		assertEquals( 1, result.size() );
		//item = (Item) distinctResult.get( 0 );

		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testUpdateQuery() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		assertTrue( em.contains( item ) );

		em.flush();
		em.clear();

		assertEquals(
				1, em.createNativeQuery(
				"update Item set descr = 'Logitech Mouse' where name = 'Mouse'"
		).executeUpdate()
		);
		item = em.find( Item.class, item.getName() );
		assertEquals( "Logitech Mouse", item.getDescr() );
		em.remove( item );
		em.getTransaction().rollback();

		em.close();

	}

	@Test
	public void testUnavailableNamedQuery() throws Exception {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		try {
			em.createNamedQuery( "wrong name" );
			fail("Wrong named query should raise an exception");
		}
		catch (IllegalArgumentException e) {
			//success
		}
		assertTrue(
				"thrown IllegalArgumentException should of caused transaction to be marked for rollback only",
				true == em.getTransaction().getRollbackOnly()
		);
		em.getTransaction().rollback();		// HHH-8442 changed to rollback since thrown ISE causes
											// transaction to be marked for rollback only.
											// No need to remove entity since it was rolled back.

		assertNull(
				"entity should not of been saved to database since IllegalArgumentException should of" +
						"caused transaction to be marked for rollback only", em.find( Item.class, item.getName() )
		);
		em.close();

	}

	@Test
	public void testTypedNamedNativeQuery() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		item = em.createNamedQuery( "nativeItem1", Item.class ).getSingleResult();
		item = em.createNamedQuery( "nativeItem2", Item.class ).getSingleResult();
		assertNotNull( item );
		assertEquals( "Micro$oft mouse", item.getDescr() );
		em.remove( item );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testTypedScalarQueries() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		Object[] itemData = em.createQuery( "select i.name,i.descr from Item i", Object[].class ).getSingleResult();
		assertEquals( 2, itemData.length );
		assertEquals( String.class, itemData[0].getClass() );
		assertEquals( String.class, itemData[1].getClass() );
		Tuple itemTuple = em.createQuery( "select i.name,i.descr from Item i", Tuple.class ).getSingleResult();
		assertEquals( 2, itemTuple.getElements().size() );
		assertEquals( String.class, itemTuple.get( 0 ).getClass() );
		assertEquals( String.class, itemTuple.get( 1 ).getClass() );
		Item itemView = em.createQuery( "select new Item(i.name,i.descr) from Item i", Item.class ).getSingleResult();
		assertNotNull( itemView );
		assertEquals( "Micro$oft mouse", itemView.getDescr() );
		itemView = em.createNamedQuery( "query-construct", Item.class ).getSingleResult();
		assertNotNull( itemView );
		assertEquals( "Micro$oft mouse", itemView.getDescr() );
		em.remove( item );
		em.getTransaction().commit();

		em.close();
	}
}
