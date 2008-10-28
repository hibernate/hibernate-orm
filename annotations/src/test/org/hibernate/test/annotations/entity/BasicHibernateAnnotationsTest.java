//$Id$
package org.hibernate.test.annotations.entity;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class BasicHibernateAnnotationsTest extends TestCase {

	public void testEntity() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		forest = (Forest) s.get( Forest.class, forest.getId() );
		assertNotNull( forest );
		forest.setName( "Fontainebleau" );
		//should not execute SQL update
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		forest = (Forest) s.get( Forest.class, forest.getId() );
		assertNotNull( forest );
		forest.setLength( 23 );
		//should execute dynamic SQL update
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( Forest.class, forest.getId() ) );
		tx.commit();
		s.close();
	}

	public void testVersioning() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		forest.setLength( 33 );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		Session parallelSession = openSession();
		Transaction parallelTx = parallelSession.beginTransaction();
		s = openSession();
		tx = s.beginTransaction();

		forest = (Forest) parallelSession.get( Forest.class, forest.getId() );
		Forest reloadedForest = (Forest) s.get( Forest.class, forest.getId() );
		reloadedForest.setLength( 11 );
		assertNotSame( forest, reloadedForest );
		tx.commit();
		s.close();

		forest.setLength( 22 );
		try {
			parallelTx.commit();
			fail( "All optimistic locking should have make it fail" );
		}
		catch (HibernateException e) {
			if ( parallelTx != null ) parallelTx.rollback();
		}
		finally {
			parallelSession.close();
		}

		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( Forest.class, forest.getId() ) );
		tx.commit();
		s.close();

	}

	public void testPolymorphism() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		forest.setLength( 33 );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query query = s.createQuery( "from java.lang.Object" );
		assertEquals( 0, query.list().size() );
		query = s.createQuery( "from Forest" );
		assertTrue( 0 < query.list().size() );
		tx.commit();
		s.close();
	}

	public void testType() throws Exception {
		Forest f = new Forest();
		f.setName( "Broceliande" );
		String description = "C'est une enorme foret enchantee ou vivais Merlin et toute la clique";
		f.setLongDescription( description );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( f );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (Forest) s.get( Forest.class, f.getId() );
		assertNotNull( f );
		assertEquals( description, f.getLongDescription() );
		s.delete( f );
		tx.commit();
		s.close();

	}

	public void testNonLazy() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Forest f = new Forest();
		Tree t = new Tree();
		t.setName( "Basic one" );
		s.persist( f );
		s.persist( t );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (Forest) s.load( Forest.class, f.getId() );
		t = (Tree) s.load( Tree.class, t.getId() );
		assertFalse( "Default should be lazy", Hibernate.isInitialized( f ) );
		assertTrue( "Tree is not lazy", Hibernate.isInitialized( t ) );
		tx.commit();
		s.close();
	}

	public void testCache() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		ZipCode zc = new ZipCode();
		zc.code = "92400";
		s.persist( zc );
		tx.commit();
		s.close();
		getSessions().getStatistics().clear();
		getSessions().getStatistics().setStatisticsEnabled( true );
		getSessions().evict( ZipCode.class );
		s = openSession();
		tx = s.beginTransaction();
		s.get( ZipCode.class, zc.code );
		assertEquals( 1, getSessions().getStatistics().getSecondLevelCachePutCount() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.get( ZipCode.class, zc.code );
		assertEquals( 1, getSessions().getStatistics().getSecondLevelCacheHitCount() );
		tx.commit();
		s.close();
	}

	public void testFilter() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete Forest" ).executeUpdate();
		Forest f1 = new Forest();
		f1.setLength( 2 );
		s.persist( f1 );
		Forest f2 = new Forest();
		f2.setLength( 20 );
		s.persist( f2 );
		Forest f3 = new Forest();
		f3.setLength( 200 );
		s.persist( f3 );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		s.enableFilter( "betweenLength" ).setParameter( "minLength", 5 ).setParameter( "maxLength", 50 );
		long count = ( (Long) s.createQuery( "select count(*) from Forest" ).iterate().next() ).intValue();
		assertEquals( 1, count );
		s.disableFilter( "betweenLength" );
		s.enableFilter( "minLength" ).setParameter( "minLength", 5 );
		count = ( (Long) s.createQuery( "select count(*) from Forest" ).iterate().next() ).longValue();
		assertEquals( 2l, count );
		s.disableFilter( "minLength" );
		tx.rollback();
		s.close();
	}

	public void testParameterizedType() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Forest f = new Forest();
		f.setSmallText( "ThisIsASmallText" );
		f.setBigText( "ThisIsABigText" );
		s.persist( f );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		Forest f2 = (Forest) s.get( Forest.class, f.getId() );
		assertEquals( f.getSmallText().toLowerCase(), f2.getSmallText() );
		assertEquals( f.getBigText().toUpperCase(), f2.getBigText() );
		tx.commit();
		s.close();
	}

	public void testSerialized() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Shire" );
		Country country = new Country();
		country.setName( "Middle Earth" );
		forest.setCountry( country );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		forest = (Forest) s.get( Forest.class, forest.getId() );
		assertNotNull( forest );
		assertNotNull( forest.getCountry() );
		assertEquals( country.getName(), forest.getCountry().getName() );
		tx.commit();
		s.close();
	}

	public void testCompositeType() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Ransom r = new Ransom();
		r.setKidnapperName( "Se7en" );
		r.setDate( new Date() );
		MonetaryAmount amount = new MonetaryAmount(
				new BigDecimal( 100000 ),
				Currency.getInstance( "EUR" )
		);
		r.setAmount( amount );
		s.persist( r );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		r = (Ransom) s.get( Ransom.class, r.getId() );
		assertNotNull( r );
		assertNotNull( r.getAmount() );
		assertTrue( 0 == new BigDecimal( 100000 ).compareTo( r.getAmount().getAmount() ) );
		assertEquals( Currency.getInstance( "EUR" ), r.getAmount().getCurrency() );
		tx.commit();
		s.close();
	}

	public void testFormula() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		org.hibernate.test.annotations.entity.Flight airFrance = new Flight();
		airFrance.setId( new Long( 747 ) );
		airFrance.setMaxAltitude( 10000 );
		s.persist( airFrance );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		airFrance = (Flight) s.get( Flight.class, airFrance.getId() );
		assertNotNull( airFrance );
		assertEquals( 10000000, airFrance.getMaxAltitudeInMilimeter() );
		s.delete( airFrance );
		tx.commit();
		s.close();
	}

	public BasicHibernateAnnotationsTest(String x) {
		super( x );
	}

	protected Class[] getMappings() {
		return new Class[]{
				Forest.class,
				Tree.class,
				Ransom.class,
				ZipCode.class,
				Flight.class
		};
	}

	protected String[] getAnnotatedPackages() {
		return new String[]{
				"org.hibernate.test.annotations.entity"
		};
	}


}
