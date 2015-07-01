/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.transform.DistinctRootEntityResultTransformer;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Implementation of DynamicFilterTest.
 *
 * @author Steve Ebersole
 */
@SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-3637")
public class DynamicFilterTest extends BaseNonConfigCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( DynamicFilterTest.class );

	@Override
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

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Override
	public void addSettings(Map settings) {
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, "1" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
	}

	@Test
	@SkipForDialect( value = {SybaseASE15Dialect.class, IngresDialect.class})
	public void testSqlSyntaxOfFiltersWithUnions() {
		Session session = openSession();
		session.enableFilter( "unioned" );
		session.createQuery( "from Category" ).list();
		session.close();
	}

	@Test
	public void testSecondLevelCachedCollectionsFiltering() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		long ts = ( ( SessionImplementor ) session ).getTimestamp();

		// Force a collection into the second level cache, with its non-filtered elements
		Salesperson sp = ( Salesperson ) session.load( Salesperson.class, testData.steveId );
		Hibernate.initialize( sp.getOrders() );
		CollectionPersister persister = sessionFactory().getCollectionPersister( Salesperson.class.getName() + ".orders" );
		assertTrue( "No cache for collection", persister.hasCache() );
		CollectionRegionAccessStrategy cache = persister.getCacheAccessStrategy();
		Object cacheKey = cache.generateCacheKey(
				testData.steveId,
				persister,
				sessionFactory(),
				session.getTenantIdentifier()
		);
		CollectionCacheEntry cachedData = ( CollectionCacheEntry ) cache.get( cacheKey, ts );
		assertNotNull( "collection was not in cache", cachedData );

		session.close();

		session = openSession();
		ts = ( ( SessionImplementor ) session ).getTimestamp();
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		sp = ( Salesperson ) session.createQuery( "from Salesperson as s where s.id = :id" )
				.setLong( "id", testData.steveId )
				.uniqueResult();
		assertEquals( "Filtered-collection not bypassing 2L-cache", 1, sp.getOrders().size() );

		Object cacheKey2 = cache.generateCacheKey(
				testData.steveId,
				persister,
				sessionFactory(),
				session.getTenantIdentifier()
		);
		CollectionCacheEntry cachedData2 = ( CollectionCacheEntry ) persister.getCacheAccessStrategy().get( cacheKey2, ts );
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

	@Test
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

		session.disableFilter( "regionlist" );
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[]{"LA", "APAC", "APAC"} );
		// Second test retreival through hql with the collection as non-eager with different region list
		salespersons = session.createQuery( "select s from Salesperson as s" ).list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
		sp = ( Salesperson ) salespersons.get( 0 );
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

	@Test
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

	@Test
	public void testFiltersWithCustomerReadAndWrite() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Custom SQL read/write with filter
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        log.info("Starting HQL filter with custom SQL get/set tests");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "heavyProducts" ).setParameter("weightKilograms", 4d);
        log.info( "HQL against Product..." );
		List results = session.createQuery( "from Product").list();
		assertEquals( 1, results.size() );

		session.close();
		testData.release();
	}

	@Test
	public void testCriteriaQueryFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-query test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        log.info("Starting Criteria-query filter tests");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "region" ).setParameter( "region", "APAC" );

		session.enableFilter( "fulfilledOrders" )
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

		session.enableFilter( "effectiveDate" )
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

        log.info("Criteria query against Salesperson...");
		List salespersons = session.createCriteria( Salesperson.class )
		        .setFetchMode( "orders", FetchMode.JOIN )
		        .list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
		assertEquals( "Incorrect order count", 1, ( ( Salesperson ) salespersons.get( 0 ) ).getOrders().size() );

        log.info("Criteria query against Product...");
		List products = session.createCriteria( Product.class )
		        .add( Restrictions.eq( "stockNumber", 124 ) )
		        .list();
		assertEquals( "Incorrect product count", 1, products.size() );

		session.close();
		testData.release();
	}

	@Test
	public void testCriteriaControl() {
		TestData testData = new TestData();
		testData.prepare();

		// the subquery...
		DetachedCriteria subquery = DetachedCriteria.forClass( Salesperson.class )
				.setProjection( Property.forName( "name" ) );

		Session session = openSession();
		session.beginTransaction();
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] {"APAC"} );

		List result = session.createCriteria( Order.class )
				.add( Subqueries.in( "steve", subquery ) )
				.list();
		assertEquals( 1, result.size() );

		session.getTransaction().commit();
		session.close();

		testData.release();
	}

	@Test
	public void testCriteriaSubqueryWithFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-subquery test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        log.info("Starting Criteria-subquery filter tests");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter("region").setParameter("region", "APAC");

        log.info("Criteria query against Department with a subquery on Salesperson in the APAC reqion...");
		DetachedCriteria salespersonSubquery = DetachedCriteria.forClass(Salesperson.class)
				.add(Restrictions.eq("name", "steve"))
				.setProjection(Property.forName("department"));

		Criteria departmentsQuery = session.createCriteria(Department.class).add(Subqueries.propertyIn("id", salespersonSubquery));
		List departments = departmentsQuery.list();

		assertEquals("Incorrect department count", 1, departments.size());

        log.info("Criteria query against Department with a subquery on Salesperson in the FooBar reqion...");

		session.enableFilter("region").setParameter("region", "Foobar");
		departments = departmentsQuery.list();

		assertEquals("Incorrect department count", 0, departments.size());

        log.info("Criteria query against Order with a subquery for line items with a subquery on product and sold by a given sales person...");
		session.enableFilter("region").setParameter("region", "APAC");

		DetachedCriteria lineItemSubquery = DetachedCriteria.forClass(LineItem.class)
				.add( Restrictions.ge( "quantity", 1L ) )
				.createCriteria( "product" )
				.add( Restrictions.eq( "name", "Acme Hair Gel" ) )
				.setProjection( Property.forName( "id" ) );

		List orders = session.createCriteria(Order.class)
				.add(Subqueries.exists(lineItemSubquery))
				.add(Restrictions.eq("buyer", "gavin"))
				.list();

		assertEquals("Incorrect orders count", 1, orders.size());

        log.info("query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month");
		session.enableFilter("region").setParameter("region", "APAC");
		session.enableFilter("effectiveDate").setParameter("asOfDate", testData.lastMonth.getTime());

		DetachedCriteria productSubquery = DetachedCriteria.forClass(Product.class)
				.add(Restrictions.eq("name", "Acme Hair Gel"))
				.setProjection(Property.forName("id"));

		lineItemSubquery = DetachedCriteria.forClass(LineItem.class)
				.add(Restrictions.ge("quantity", 1L ))
				.createCriteria("product")
				.add(Subqueries.propertyIn("id", productSubquery))
				.setProjection(Property.forName("id"));

		orders = session.createCriteria(Order.class)
				.add(Subqueries.exists(lineItemSubquery))
				.add(Restrictions.eq("buyer", "gavin"))
				.list();

		assertEquals("Incorrect orders count", 1, orders.size());


        log.info("query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of 4 months ago");
		session.enableFilter("region").setParameter("region", "APAC");
		session.enableFilter("effectiveDate").setParameter("asOfDate", testData.fourMonthsAgo.getTime());

		orders = session.createCriteria(Order.class)
				.add(Subqueries.exists(lineItemSubquery))
				.add(Restrictions.eq("buyer", "gavin"))
				.list();

		assertEquals("Incorrect orders count", 0, orders.size());

		session.close();
		testData.release();
	}

	@Test
	public void testHQLSubqueryWithFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// HQL subquery with filters test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        log.info("Starting HQL subquery with filters tests");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter("region").setParameter("region", "APAC");

        log.info("query against Department with a subquery on Salesperson in the APAC reqion...");

		List departments = session.createQuery(
				"select d from Department as d where d.id in (select s.department from Salesperson s where s.name = ?)"
		).setString( 0, "steve" ).list();

		assertEquals("Incorrect department count", 1, departments.size());

        log.info("query against Department with a subquery on Salesperson in the FooBar reqion...");

		session.enableFilter("region").setParameter( "region", "Foobar" );
		departments = session.createQuery("select d from Department as d where d.id in (select s.department from Salesperson s where s.name = ?)").setString(0, "steve").list();

		assertEquals( "Incorrect department count", 0, departments.size() );

        log.info("query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region for a given buyer");
		session.enableFilter("region").setParameter( "region", "APAC" );

		List orders = session.createQuery("select o from Order as o where exists (select li.id from LineItem li, Product as p where p.id = li.product and li.quantity >= ? and p.name = ?) and o.buyer = ?")
				.setLong(0, 1L).setString(1, "Acme Hair Gel").setString(2, "gavin").list();

		assertEquals( "Incorrect orders count", 1, orders.size() );

        log.info("query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month");

		session.enableFilter("region").setParameter("region", "APAC");
		session.enableFilter("effectiveDate").setParameter( "asOfDate", testData.lastMonth.getTime() );

		orders = session.createQuery("select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ? and li.product in (select p.id from Product p where p.name = ?)) and o.buyer = ?")
				.setLong(0, 1L).setString(1, "Acme Hair Gel").setString(2, "gavin").list();

		assertEquals( "Incorrect orders count", 1, orders.size() );


        log.info(
				"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of 4 months ago"
		);

		session.enableFilter("region").setParameter("region", "APAC");
		session.enableFilter("effectiveDate").setParameter("asOfDate", testData.fourMonthsAgo.getTime());

		orders = session.createQuery("select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ? and li.product in (select p.id from Product p where p.name = ?)) and o.buyer = ?")
				.setLong( 0, 1L ).setString( 1, "Acme Hair Gel" ).setString( 2, "gavin" ).list();

		assertEquals("Incorrect orders count", 0, orders.size());

        log.info("query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month with named types");

		session.enableFilter("region").setParameter("region", "APAC");
		session.enableFilter("effectiveDate").setParameter("asOfDate", testData.lastMonth.getTime());

		orders = session.createQuery("select o from Order as o where exists (select li.id from LineItem li where li.quantity >= :quantity and li.product in (select p.id from Product p where p.name = :name)) and o.buyer = :buyer")
				.setLong("quantity", 1L).setString("name", "Acme Hair Gel").setString("buyer", "gavin").list();

		assertEquals("Incorrect orders count", 1, orders.size());

        log.info("query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month with mixed types");

		session.enableFilter("region").setParameter("region", "APAC");
		session.enableFilter("effectiveDate").setParameter("asOfDate", testData.lastMonth.getTime());

		orders = session.createQuery("select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ? and li.product in (select p.id from Product p where p.name = ?)) and o.buyer = :buyer")
				.setLong( 0, 1L ).setString( 1, "Acme Hair Gel" ).setString( "buyer", "gavin" ).list();

		assertEquals("Incorrect orders count", 1, orders.size());

		session.close();
		testData.release();
	}

	@Test
	public void testFilterApplicationOnHqlQueryWithImplicitSubqueryContainingPositionalParameter() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.beginTransaction();

		final String queryString = "from Order o where ? in ( select sp.name from Salesperson sp )";

		// first a control-group query
		List result = session.createQuery( queryString ).setParameter( 0, "steve" ).list();
		assertEquals( 2, result.size() );

		// now lets enable filters on Order...
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		result = session.createQuery( queryString ).setParameter( 0, "steve" ).list();
		assertEquals( 1, result.size() );

		// now, lets additionally enable filter on Salesperson.  First a valid one...
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "APAC" } );
		result = session.createQuery( queryString ).setParameter( 0, "steve" ).list();
		assertEquals( 1, result.size() );

		// ... then a silly one...
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "gamma quadrant" } );
		result = session.createQuery( queryString ).setParameter( 0, "steve" ).list();
		assertEquals( 0, result.size() );

		session.getTransaction().commit();
		session.close();

		testData.release();
	}

	@Test
	public void testFilterApplicationOnHqlQueryWithImplicitSubqueryContainingNamedParameter() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.beginTransaction();

		final String queryString = "from Order o where :salesPersonName in ( select sp.name from Salesperson sp )";

		// first a control-group query
		List result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
		assertEquals( 2, result.size() );

		// now lets enable filters on Order...
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
		assertEquals( 1, result.size() );

		// now, lets additionally enable filter on Salesperson.  First a valid one...
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "APAC" } );
		result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
		assertEquals( 1, result.size() );

		// ... then a silly one...
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "gamma quadrant" } );
		result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
		assertEquals( 0, result.size() );

		session.getTransaction().commit();
		session.close();

		testData.release();
	}

	@Test
	public void testFiltersOnSimpleHqlDelete() {
		Session session = openSession();
		session.beginTransaction();
		Salesperson sp = new Salesperson();
		sp.setName( "steve" );
		sp.setRegion( "NA" );
		session.persist( sp );
		Salesperson sp2 = new Salesperson();
		sp2.setName( "john" );
		sp2.setRegion( "APAC" );
		session.persist( sp2 );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.enableFilter( "region" ).setParameter( "region", "NA" );
		int count = session.createQuery( "delete from Salesperson" ).executeUpdate();
		assertEquals( 1, count );
		session.delete( sp2 );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testFiltersOnMultiTableHqlDelete() {
		Session session = openSession();
		session.beginTransaction();
		Salesperson sp = new Salesperson();
		sp.setName( "steve" );
		sp.setRegion( "NA" );
		session.persist( sp );
		Salesperson sp2 = new Salesperson();
		sp2.setName( "john" );
		sp2.setRegion( "APAC" );
		session.persist( sp2 );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.enableFilter( "region" ).setParameter( "region", "NA" );
		int count = session.createQuery( "delete from Salesperson" ).executeUpdate();
		assertEquals( 1, count );
		session.delete( sp2 );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testGetFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Get() test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        log.info("Starting get() filter tests (eager assoc. fetching).");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "region" ).setParameter( "region", "APAC" );

        log.info("Performing get()...");
		Salesperson salesperson = ( Salesperson ) session.get( Salesperson.class, testData.steveId );
		assertNotNull( salesperson );
		assertEquals( "Incorrect order count", 1, salesperson.getOrders().size() );

		session.close();
		testData.release();
	}

	@Test
	public void testOneToManyFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        log.info("Starting one-to-many collection loader filter tests.");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "seniorSalespersons" )
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

        log.info("Performing load of Department...");
		Department department = ( Department ) session.load( Department.class, testData.deptId );
		Set salespersons = department.getSalespersons();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );

		session.close();
		testData.release();
	}

	@Test
	public void testInStyleFilterParameter() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        log.info("Starting one-to-many collection loader filter tests.");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "regionlist" )
		        .setParameterList( "regions", new String[]{"LA", "APAC"} );

        log.debug("Performing query of Salespersons");
		List salespersons = session.createQuery( "from Salesperson" ).list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );

		session.close();
		testData.release();
	}

	@Test
	public void testManyToManyFilterOnCriteria() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

		Product prod = ( Product ) session.createCriteria( Product.class )
		        .setResultTransformer( DistinctRootEntityResultTransformer.INSTANCE )
		        .add( Restrictions.eq( "id", testData.prod1Id ) )
		        .uniqueResult();

		assertNotNull( prod );
		assertEquals( "Incorrect Product.categories count for filter", 1, prod.getCategories().size() );

		session.close();
		testData.release();
	}

	@Test
	public void testManyToManyFilterOnLoad() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

		Product prod = ( Product ) session.get( Product.class, testData.prod1Id );

		long initLoadCount = sessionFactory().getStatistics().getCollectionLoadCount();
		long initFetchCount = sessionFactory().getStatistics().getCollectionFetchCount();

		// should already have been initialized...
		int size = prod.getCategories().size();
		assertEquals( "Incorrect filtered collection count", 1, size );

		long currLoadCount = sessionFactory().getStatistics().getCollectionLoadCount();
		long currFetchCount = sessionFactory().getStatistics().getCollectionFetchCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger join fetch",
		        ( initLoadCount == currLoadCount ) && ( initFetchCount == currFetchCount )
		);

		// make sure we did not get back a collection of proxies
		long initEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();
		Iterator itr = prod.getCategories().iterator();
		while ( itr.hasNext() ) {
			Category cat = ( Category ) itr.next();
			System.out.println( " ===> " + cat.getName() );
		}
		long currEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger *complete* join fetch",
		        ( initEntityLoadCount == currEntityLoadCount )
		);

		session.close();
		testData.release();
	}

	@Test
	public void testManyToManyOnCollectionLoadAfterHQL() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

		// Force the categories to not get initialized here
		List result = session.createQuery( "from Product as p where p.id = :id" )
		        .setLong( "id", testData.prod1Id )
		        .list();
		assertTrue( "No products returned from HQL", !result.isEmpty() );

		Product prod = ( Product ) result.get( 0 );
		assertNotNull( prod );
		assertEquals( "Incorrect Product.categories count for filter on collection load", 1, prod.getCategories().size() );

		session.close();
		testData.release();
	}

	@Test
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

	@Test
	public void testManyToManyBase() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();

		Product prod = ( Product ) session.get( Product.class, testData.prod1Id );

		long initLoadCount = sessionFactory().getStatistics().getCollectionLoadCount();
		long initFetchCount = sessionFactory().getStatistics().getCollectionFetchCount();

		// should already have been initialized...
		int size = prod.getCategories().size();
		assertEquals( "Incorrect non-filtered collection count", 2, size );

		long currLoadCount = sessionFactory().getStatistics().getCollectionLoadCount();
		long currFetchCount = sessionFactory().getStatistics().getCollectionFetchCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger join fetch",
		        ( initLoadCount == currLoadCount ) && ( initFetchCount == currFetchCount )
		);

		// make sure we did not get back a collection of proxies
		long initEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();
		Iterator itr = prod.getCategories().iterator();
		while ( itr.hasNext() ) {
			Category cat = ( Category ) itr.next();
			System.out.println( " ===> " + cat.getName() );
		}
		long currEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger *complete* join fetch",
		        ( initEntityLoadCount == currEntityLoadCount )
		);

		session.close();
		testData.release();
	}

	@Test
	public void testManyToManyBaseThruCriteria() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();

		List result = session.createCriteria( Product.class )
		        .add( Restrictions.eq( "id", testData.prod1Id ) )
		        .list();

		Product prod = ( Product ) result.get( 0 );

		long initLoadCount = sessionFactory().getStatistics().getCollectionLoadCount();
		long initFetchCount = sessionFactory().getStatistics().getCollectionFetchCount();

		// should already have been initialized...
		int size = prod.getCategories().size();
		assertEquals( "Incorrect non-filtered collection count", 2, size );

		long currLoadCount = sessionFactory().getStatistics().getCollectionLoadCount();
		long currFetchCount = sessionFactory().getStatistics().getCollectionFetchCount();

		assertTrue(
		        "load with join fetch of many-to-many did not trigger join fetch",
		        ( initLoadCount == currLoadCount ) && ( initFetchCount == currFetchCount )
		);

		// make sure we did not get back a collection of proxies
		long initEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();
		Iterator itr = prod.getCategories().iterator();
		while ( itr.hasNext() ) {
			Category cat = ( Category ) itr.next();
			System.out.println( " ===> " + cat.getName() );
		}
		long currEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();

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
			product1.setWeightPounds( 0.25 );
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
			product1.setWeightPounds( 10.0 );
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
