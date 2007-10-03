// $Id: DynamicFilterTest.java 11398 2007-04-10 14:54:07Z steve.ebersole@jboss.com $
package org.hibernate.test.filter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.entry.CollectionCacheEntry;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.transform.DistinctRootEntityResultTransformer;

/**
 * Implementation of DynamicFilterTest.
 *
 * @author Steve
 */
public class DynamicFilterTest extends FunctionalTestCase {

	private Logger log = LoggerFactory.getLogger( DynamicFilterTest.class );

	public DynamicFilterTest(String testName) {
		super( testName );
	}

	public String[] getMappings() {
		return new String[]{
			"filter/defs.hbm.xml",
			"filter/LineItem.hbm.xml",
			"filter/Order.hbm.xml",
			"filter/Product.hbm.xml",
			"filter/Salesperson.hbm.xml",
			"filter/Department.hbm.xml",
			"filter/Category.hbm.xml"
		};
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.MAX_FETCH_DEPTH, "1" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( DynamicFilterTest.class );
	}

	public void testSqlSyntaxOfFiltersWithUnions() {
		Session session = openSession();
		session.enableFilter( "unioned" );
		session.createQuery( "from Category" ).list();
		session.close();
	}

	public void testSecondLevelCachedCollectionsFiltering() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		long ts = ( ( SessionImplementor ) session ).getTimestamp();

		// Force a collection into the second level cache, with its non-filtered elements
		Salesperson sp = ( Salesperson ) session.load( Salesperson.class, testData.steveId );
		Hibernate.initialize( sp.getOrders() );
		CollectionPersister persister = ( ( SessionFactoryImpl ) getSessions() )
		        .getCollectionPersister( Salesperson.class.getName() + ".orders" );
		assertTrue( "No cache for collection", persister.hasCache() );
		CollectionCacheEntry cachedData = ( CollectionCacheEntry ) persister.getCacheAccessStrategy()
		        .get( new CacheKey( testData.steveId, persister.getKeyType(), persister.getRole(), EntityMode.POJO, sfi() ), ts );
		assertNotNull( "collection was not in cache", cachedData );

		session.close();

		session = openSession();
		ts = ( ( SessionImplementor ) session ).getTimestamp();
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		sp = ( Salesperson ) session.createQuery( "from Salesperson as s where s.id = :id" )
		        .setLong( "id", testData.steveId.longValue() )
		        .uniqueResult();
		assertEquals( "Filtered-collection not bypassing 2L-cache", 1, sp.getOrders().size() );

		CollectionCacheEntry cachedData2 = ( CollectionCacheEntry ) persister.getCacheAccessStrategy()
		        .get( new CacheKey( testData.steveId, persister.getKeyType(), persister.getRole(), EntityMode.POJO, sfi() ), ts );
		assertNotNull( "collection no longer in cache!", cachedData2 );
		assertSame( "Different cache values!", cachedData, cachedData2 );

		session.close();

		session = openSession();
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		sp = ( Salesperson ) session.load( Salesperson.class, testData.steveId );
		assertEquals( "Filtered-collection not bypassing 2L-cache", 1, sp.getOrders().size() );

		session.close();

		// Finally, make sure that the original cached version did not get over-written
		session = openSession();
		sp = ( Salesperson ) session.load( Salesperson.class, testData.steveId );
		assertEquals( "Actual cached version got over-written", 2, sp.getOrders().size() );

		session.close();
		testData.release();
	}

	public void testCombinedClassAndCollectionFiltersEnabled() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[]{"LA", "APAC"} );
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

		// test retreival through hql with the collection as non-eager
		List salespersons = session.createQuery( "select s from Salesperson as s" ).list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
		Salesperson sp = ( Salesperson ) salespersons.get( 0 );
		assertEquals( "Incorrect order count", 1, sp.getOrders().size() );

		session.clear();

		// test retreival through hql with the collection join fetched
		salespersons = session.createQuery( "select s from Salesperson as s left join fetch s.orders" ).list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
		sp = ( Salesperson ) salespersons.get( 0 );
		assertEquals( "Incorrect order count", 1, sp.getOrders().size() );

		session.close();
		testData.release();
	}

	public void testHqlFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// HQL test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting HQL filter tests" );
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "region" ).setParameter( "region", "APAC" );

		session.enableFilter( "effectiveDate" )
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

		log.info( "HQL against Salesperson..." );
		List results = session.createQuery( "select s from Salesperson as s left join fetch s.orders" ).list();
		assertTrue( "Incorrect filtered HQL result count [" + results.size() + "]", results.size() == 1 );
		Salesperson result = ( Salesperson ) results.get( 0 );
		assertTrue( "Incorrect collectionfilter count", result.getOrders().size() == 1 );

		log.info( "HQL against Product..." );
		results = session.createQuery( "from Product as p where p.stockNumber = ?" ).setInteger( 0, 124 ).list();
		assertTrue( results.size() == 1 );

		session.close();
		testData.release();
	}

	public void testCriteriaQueryFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-query test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting Criteria-query filter tests" );
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "region" ).setParameter( "region", "APAC" );

		session.enableFilter( "fulfilledOrders" )
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

		session.enableFilter( "effectiveDate" )
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

		log.info( "Criteria query against Salesperson..." );
		List salespersons = session.createCriteria( Salesperson.class )
		        .setFetchMode( "orders", FetchMode.JOIN )
		        .list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
		assertEquals( "Incorrect order count", 1, ( ( Salesperson ) salespersons.get( 0 ) ).getOrders().size() );

		log.info( "Criteria query against Product..." );
		List products = session.createCriteria( Product.class )
		        .add( Restrictions.eq( "stockNumber", new Integer( 124 ) ) )
		        .list();
		assertEquals( "Incorrect product count", 1, products.size() );

		session.close();
		testData.release();
	}

	public void testGetFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Get() test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting get() filter tests (eager assoc. fetching)." );
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "region" ).setParameter( "region", "APAC" );

		log.info( "Performing get()..." );
		Salesperson salesperson = ( Salesperson ) session.get( Salesperson.class, testData.steveId );
		assertNotNull( salesperson );
		assertEquals( "Incorrect order count", 1, salesperson.getOrders().size() );

		session.close();
		testData.release();
	}

	public void testOneToManyFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting one-to-many collection loader filter tests." );
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "seniorSalespersons" )
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

		log.info( "Performing load of Department..." );
		Department department = ( Department ) session.load( Department.class, testData.deptId );
		Set salespersons = department.getSalespersons();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );

		session.close();
		testData.release();
	}

	public void testInStyleFilterParameter() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting one-to-many collection loader filter tests." );
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "regionlist" )
		        .setParameterList( "regions", new String[]{"LA", "APAC"} );

		log.debug( "Performing query of Salespersons" );
		List salespersons = session.createQuery( "from Salesperson" ).list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );

		session.close();
		testData.release();
	}

	public void testManyToManyFilterOnCriteria() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

		Product prod = ( Product ) session.createCriteria( Product.class )
		        .setResultTransformer( new DistinctRootEntityResultTransformer() )
		        .add( Restrictions.eq( "id", testData.prod1Id ) )
		        .uniqueResult();

		assertNotNull( prod );
		assertEquals( "Incorrect Product.categories count for filter", 1, prod.getCategories().size() );

		session.close();
		testData.release();
	}

	public void testManyToManyFilterOnLoad() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

		Product prod = ( Product ) session.get( Product.class, testData.prod1Id );

		long initLoadCount = getSessions().getStatistics().getCollectionLoadCount();
		long initFetchCount = getSessions().getStatistics().getCollectionFetchCount();

		// should already have been initialized...
		int size = prod.getCategories().size();
		assertEquals( "Incorrect filtered collection count", 1, size );

		long currLoadCount = getSessions().getStatistics().getCollectionLoadCount();
		long currFetchCount = getSessions().getStatistics().getCollectionFetchCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger join fetch",
		        ( initLoadCount == currLoadCount ) && ( initFetchCount == currFetchCount )
		);

		// make sure we did not get back a collection of proxies
		long initEntityLoadCount = getSessions().getStatistics().getEntityLoadCount();
		Iterator itr = prod.getCategories().iterator();
		while ( itr.hasNext() ) {
			Category cat = ( Category ) itr.next();
			System.out.println( " ===> " + cat.getName() );
		}
		long currEntityLoadCount = getSessions().getStatistics().getEntityLoadCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger *complete* join fetch",
		        ( initEntityLoadCount == currEntityLoadCount )
		);

		session.close();
		testData.release();
	}

	public void testManyToManyOnCollectionLoadAfterHQL() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

		// Force the categories to not get initialized here
		List result = session.createQuery( "from Product as p where p.id = :id" )
		        .setLong( "id", testData.prod1Id.longValue() )
		        .list();
		assertTrue( "No products returned from HQL", !result.isEmpty() );

		Product prod = ( Product ) result.get( 0 );
		assertNotNull( prod );
		assertEquals( "Incorrect Product.categories count for filter on collection load", 1, prod.getCategories().size() );

		session.close();
		testData.release();
	}

	public void testManyToManyFilterOnQuery() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

		List result = session.createQuery( "from Product p inner join fetch p.categories" ).list();
		assertTrue( "No products returned from HQL many-to-many filter case", !result.isEmpty() );

		Product prod = ( Product ) result.get( 0 );

		assertNotNull( prod );
		assertEquals( "Incorrect Product.categories count for filter with HQL", 1, prod.getCategories().size() );

		session.close();
		testData.release();
	}

	public void testManyToManyBase() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();

		Product prod = ( Product ) session.get( Product.class, testData.prod1Id );

		long initLoadCount = getSessions().getStatistics().getCollectionLoadCount();
		long initFetchCount = getSessions().getStatistics().getCollectionFetchCount();

		// should already have been initialized...
		int size = prod.getCategories().size();
		assertEquals( "Incorrect non-filtered collection count", 2, size );

		long currLoadCount = getSessions().getStatistics().getCollectionLoadCount();
		long currFetchCount = getSessions().getStatistics().getCollectionFetchCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger join fetch",
		        ( initLoadCount == currLoadCount ) && ( initFetchCount == currFetchCount )
		);

		// make sure we did not get back a collection of proxies
		long initEntityLoadCount = getSessions().getStatistics().getEntityLoadCount();
		Iterator itr = prod.getCategories().iterator();
		while ( itr.hasNext() ) {
			Category cat = ( Category ) itr.next();
			System.out.println( " ===> " + cat.getName() );
		}
		long currEntityLoadCount = getSessions().getStatistics().getEntityLoadCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger *complete* join fetch",
		        ( initEntityLoadCount == currEntityLoadCount )
		);

		session.close();
		testData.release();
	}

	public void testManyToManyBaseThruCriteria() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();

		List result = session.createCriteria( Product.class )
		        .add( Restrictions.eq( "id", testData.prod1Id ) )
		        .list();

		Product prod = ( Product ) result.get( 0 );

		long initLoadCount = getSessions().getStatistics().getCollectionLoadCount();
		long initFetchCount = getSessions().getStatistics().getCollectionFetchCount();

		// should already have been initialized...
		int size = prod.getCategories().size();
		assertEquals( "Incorrect non-filtered collection count", 2, size );

		long currLoadCount = getSessions().getStatistics().getCollectionLoadCount();
		long currFetchCount = getSessions().getStatistics().getCollectionFetchCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger join fetch",
		        ( initLoadCount == currLoadCount ) && ( initFetchCount == currFetchCount )
		);

		// make sure we did not get back a collection of proxies
		long initEntityLoadCount = getSessions().getStatistics().getEntityLoadCount();
		Iterator itr = prod.getCategories().iterator();
		while ( itr.hasNext() ) {
			Category cat = ( Category ) itr.next();
			System.out.println( " ===> " + cat.getName() );
		}
		long currEntityLoadCount = getSessions().getStatistics().getEntityLoadCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger *complete* join fetch",
		        ( initEntityLoadCount == currEntityLoadCount )
		);

		session.close();
		testData.release();
	}

	private class TestData {
		private Long steveId;
		private Long deptId;
		private Long prod1Id;
		private Calendar lastMonth;
		private Calendar nextMonth;
		private Calendar sixMonthsAgo;
		private Calendar fourMonthsAgo;

		private List entitiesToCleanUp = new ArrayList();

		private void prepare() {
			Session session = openSession();
			Transaction transaction = session.beginTransaction();

			lastMonth = new GregorianCalendar();
			lastMonth.add( Calendar.MONTH, -1 );

			nextMonth = new GregorianCalendar();
			nextMonth.add( Calendar.MONTH, 1 );

			sixMonthsAgo = new GregorianCalendar();
			sixMonthsAgo.add( Calendar.MONTH, -6 );

			fourMonthsAgo = new GregorianCalendar();
			fourMonthsAgo.add( Calendar.MONTH, -4 );

			Department dept = new Department();
			dept.setName( "Sales" );

			session.save( dept );
			deptId = dept.getId();
			entitiesToCleanUp.add( dept );

			Salesperson steve = new Salesperson();
			steve.setName( "steve" );
			steve.setRegion( "APAC" );
			steve.setHireDate( sixMonthsAgo.getTime() );

			steve.setDepartment( dept );
			dept.getSalespersons().add( steve );

			Salesperson max = new Salesperson();
			max.setName( "max" );
			max.setRegion( "EMEA" );
			max.setHireDate( nextMonth.getTime() );

			max.setDepartment( dept );
			dept.getSalespersons().add( max );

			session.save( steve );
			session.save( max );
			entitiesToCleanUp.add( steve );
			entitiesToCleanUp.add( max );

			steveId = steve.getId();

			Category cat1 = new Category( "test cat 1", lastMonth.getTime(), nextMonth.getTime() );
			Category cat2 = new Category( "test cat 2", sixMonthsAgo.getTime(), fourMonthsAgo.getTime() );

			Product product1 = new Product();
			product1.setName( "Acme Hair Gel" );
			product1.setStockNumber( 123 );
			product1.setEffectiveStartDate( lastMonth.getTime() );
			product1.setEffectiveEndDate( nextMonth.getTime() );

			product1.addCategory( cat1 );
			product1.addCategory( cat2 );

			session.save( product1 );
			entitiesToCleanUp.add( product1 );
			prod1Id = product1.getId();

			Order order1 = new Order();
			order1.setBuyer( "gavin" );
			order1.setRegion( "APAC" );
			order1.setPlacementDate( sixMonthsAgo.getTime() );
			order1.setFulfillmentDate( fourMonthsAgo.getTime() );
			order1.setSalesperson( steve );
			order1.addLineItem( product1, 500 );

			session.save( order1 );
			entitiesToCleanUp.add( order1 );

			Product product2 = new Product();
			product2.setName( "Acme Super-Duper DTO Factory" );
			product2.setStockNumber( 124 );
			product2.setEffectiveStartDate( sixMonthsAgo.getTime() );
			product2.setEffectiveEndDate( new Date() );

			Category cat3 = new Category( "test cat 2", sixMonthsAgo.getTime(), new Date() );
			product2.addCategory( cat3 );

			session.save( product2 );
			entitiesToCleanUp.add( product2 );

			// An uncategorized product
			Product product3 = new Product();
			product3.setName( "Uncategorized product" );
			session.save( product3 );
			entitiesToCleanUp.add( product3 );

			Order order2 = new Order();
			order2.setBuyer( "christian" );
			order2.setRegion( "EMEA" );
			order2.setPlacementDate( lastMonth.getTime() );
			order2.setSalesperson( steve );
			order2.addLineItem( product2, -1 );

			session.save( order2 );
			entitiesToCleanUp.add( order2 );

			transaction.commit();
			session.close();
		}

		private void release() {
			Session session = openSession();
			Transaction transaction = session.beginTransaction();

			Iterator itr = entitiesToCleanUp.iterator();
			while ( itr.hasNext() ) {
				session.delete( itr.next() );
			}

			transaction.commit();
			session.close();
		}
	}
}
