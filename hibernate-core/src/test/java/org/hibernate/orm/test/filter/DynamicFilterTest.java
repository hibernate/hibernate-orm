/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import jakarta.persistence.criteria.JoinType;
import org.hibernate.Hibernate;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.CacheSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY;
import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.FetchSettings.MAX_FETCH_DEPTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Implementation of DynamicFilterTest.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = {
		@Setting(name = MAX_FETCH_DEPTH, value = "1"),
		@Setting(name = USE_QUERY_CACHE, value = "true"),
		@Setting(name = DEFAULT_CACHE_CONCURRENCY_STRATEGY, value = DynamicFilterTest.CACHE_ACCESS_STRATEGY)
})
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/filter/defs.hbm.xml",
		"org/hibernate/orm/test/filter/LineItem.hbm.xml",
		"org/hibernate/orm/test/filter/Order.hbm.xml",
		"org/hibernate/orm/test/filter/Product.hbm.xml",
		"org/hibernate/orm/test/filter/Salesperson.hbm.xml",
		"org/hibernate/orm/test/filter/Department.hbm.xml",
		"org/hibernate/orm/test/filter/Category.hbm.xml"
})
@SessionFactory(generateStatistics = true)
public class DynamicFilterTest {
	private static final Logger log = Logger.getLogger( DynamicFilterTest.class );
	public static final String CACHE_ACCESS_STRATEGY = "nonstrict-read-write";

	private TestData testData;

	@BeforeEach
	public void createTestData(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		for ( PersistentClass entityBinding : modelScope.getDomainModel().getEntityBindings() ) {
			if ( !entityBinding.isInherited() ) {
				entityBinding.getRootClass().setCacheConcurrencyStrategy( CACHE_ACCESS_STRATEGY );
				entityBinding.setCached( true );
			}
		}

		for ( Collection collectionBinding : modelScope.getDomainModel().getCollectionBindings() ) {
			collectionBinding.setCacheConcurrencyStrategy( CACHE_ACCESS_STRATEGY );
		}

		testData = new TestData();
		testData.prepare( factoryScope );
	}

	@AfterEach
	public void releaseTestData(SessionFactoryScope factoryScope) {
		testData.release( factoryScope );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnionInSubquery.class)
	public void testSqlSyntaxOfFiltersWithUnions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.enableFilter( "unioned" );
			//noinspection deprecation
			session.createQuery( "from Category" ).list();
		} );
	}

	@Test
	public void testSecondLevelCachedCollectionsFiltering(SessionFactoryScope factoryScope) {
		var sessionFactory = factoryScope.getSessionFactory();
		var persister = sessionFactory
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor(Salesperson.class.getName() + ".orders");
		var cache = persister.getCacheAccessStrategy();

		var cachedData = factoryScope.fromSession(session -> {
			// Force a collection into the second level cache, with its non-filtered elements
			var sp = session.getReference( Salesperson.class, testData.steveId );
			Hibernate.initialize( sp.getOrders() );
			assertTrue( persister.hasCache(), "No cache for collection" );
			var cacheKey = cache.generateCacheKey(
					testData.steveId,
					persister,
					sessionFactory,
					session.getTenantIdentifier()
			);
			var cached = (CollectionCacheEntry) cache.get(
					session,
					cacheKey
			);
			assertNotNull( cached, "collection was not in cache" );
			return cached;
		} );

		factoryScope.inSession(session -> {
			session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
			//noinspection deprecation
			var sp = (Salesperson) session.createQuery( "from Salesperson as s where s.id = :id" )
					.setParameter( "id", testData.steveId )
					.uniqueResult();
			assertEquals( 1, sp.getOrders().size(), "Filtered-collection not bypassing 2L-cache" );

			var cacheKey2 = cache.generateCacheKey(
					testData.steveId,
					persister,
					sessionFactory,
					session.getTenantIdentifier()
			);
			var cachedData2 = (CollectionCacheEntry) persister.getCacheAccessStrategy()
					.get( session, cacheKey2 );
			assertNotNull( cachedData2, "collection no longer in cache!" );
			assertSame( cachedData, cachedData2, "Different cache values!" );
		} );

		factoryScope.inSession(session -> {
			session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
			var sp = session.getReference( Salesperson.class, testData.steveId );
			assertEquals( 1, sp.getOrders().size(), "Filtered-collection not bypassing 2L-cache" );
		} );

		// Finally, make sure that the original cached version did not get over-written
		factoryScope.inSession(session -> {
			var sp = session.getReference( Salesperson.class, testData.steveId );
			assertEquals( 2, sp.getOrders().size(), "Actual cached version got over-written" );
		} );
	}

	@Test
	public void testCombinedClassAndCollectionFiltersEnabled(SessionFactoryScope factoryScope) {
		factoryScope.inSession(session -> {
			session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "LA", "APAC" } );
			session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

			// test retrieval through hql with the collection as non-eager
			//noinspection removal
			var salespersons = session.createQuery(
					"select s from Salesperson as s",
					Salesperson.class
			)
					.getResultList();
			assertEquals( 1, salespersons.size(), "Incorrect salesperson count" );
			var sp = salespersons.get( 0 );
			assertEquals( 1, sp.getOrders().size(), "Incorrect order count" );

			session.clear();

			session.disableFilter( "regionlist" );
			session.enableFilter( "regionlist" ).setParameterList(
					"regions",
					new String[] { "LA", "APAC", "APAC" }
			);
			// Second test retrieval through hql with the collection as non-eager with different region list
			//noinspection removal
			salespersons = session.createQuery( "select s from Salesperson as s", Salesperson.class )
					.getResultList();
			assertEquals( 1, salespersons.size(), "Incorrect salesperson count" );
			sp = salespersons.get( 0 );
			assertEquals( 1, sp.getOrders().size(), "Incorrect order count" );

			session.clear();

			// test retrieval through hql with the collection join fetched
			//noinspection removal
			salespersons = session.createQuery(
					"select s from Salesperson as s left join fetch s.orders",
					Salesperson.class
			).getResultList();
			assertEquals( 1, salespersons.size(), "Incorrect salesperson count" );
			sp = salespersons.get( 0 );
			assertEquals( 1, sp.getOrders().size(), "Incorrect order count" );
		} );
	}

	@Test
	public void testHqlFilters(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// HQL test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting HQL filter tests" );

		factoryScope.inSession(session -> {
			session.enableFilter( "region" ).setParameter( "region", "APAC" );

			session.enableFilter( "effectiveDate" )
					.setParameter( "asOfDate", testData.lastMonth.getTime() );

			log.info( "HQL against Salesperson..." );
			//noinspection deprecation
			var results = session.createQuery( "select s from Salesperson as s left join fetch s.orders" )
					.list();
			assertEquals( 1, results.size(),
					"Incorrect filtered HQL result count [" + results.size() + "]" );
			var result = (Salesperson) results.get( 0 );
			assertEquals( 1, result.getOrders().size(), "Incorrect collectionfilter count" );

			log.info( "HQL against Product..." );
			//noinspection deprecation
			results = session.createQuery( "from Product as p where p.stockNumber = ?1" )
					.setParameter( 1, 124 )
					.list();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-14567")
	public void testHqlFiltersAppliedAfterQueryCreation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			//noinspection removal
			var query = session.createQuery(
					"select s from Salesperson s",
					Salesperson.class
			);
			var list = query.list();
			assertThat( list ).hasSize( 2 );

			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			assertThat( query.list() ).hasSize( 1 );
		} );
	}

	@Test
	public void testFiltersWithCustomerReadAndWrite(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Custom SQL read/write with filter
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting HQL filter with custom SQL get/set tests" );

		factoryScope.inSession(session -> {
			session.enableFilter( "heavyProducts" ).setParameter( "weightKilograms", 4d );
			log.info( "HQL against Product..." );
			//noinspection removal
			var results = session.createQuery( "from Product", Product.class ).getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testCriteriaQueryFilters(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-query test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting Criteria-query filter tests" );

		factoryScope.inSession(session -> {
			session.enableFilter( "region" ).setParameter( "region", "APAC" );

			session.enableFilter( "fulfilledOrders" )
					.setParameter( "asOfDate", testData.lastMonth.getTime() );

			session.enableFilter( "effectiveDate" )
					.setParameter( "asOfDate", testData.lastMonth.getTime() );

			log.info( "Criteria query against Salesperson..." );
			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Salesperson.class );
			var from = criteria.from( Salesperson.class );
			from.fetch( "orders", JoinType.LEFT );
			//noinspection removal
			var salespersons = session.createQuery( criteria ).getResultList();
			assertEquals( 1, salespersons.size(), "Incorrect salesperson count" );
			assertEquals( 1, ( salespersons.get( 0 ) ).getOrders().size(), "Incorrect order count" );

			log.info( "Criteria query against Product..." );
			var productCriteria = criteriaBuilder.createQuery( Product.class );
			var productRoot = productCriteria.from( Product.class );
			productCriteria.where( criteriaBuilder.equal( productRoot.get( "stockNumber" ), 124 ) );

			//noinspection removal
			var products = session.createQuery( productCriteria ).getResultList();
			assertEquals( 1, products.size(), "Incorrect product count" );
		} );
	}

	@Test
	public void testCriteriaControl(SessionFactoryScope factoryScope) {
		var sessionFactory = factoryScope.getSessionFactory();

		// the subquery...
		var detachedCriteriaBuilder = sessionFactory.getCriteriaBuilder();
		var query = detachedCriteriaBuilder.createQuery( Salesperson.class );
		var subquery = query.subquery( String.class );
		var salespersonRoot = subquery.from( Salesperson.class );
		subquery.select( salespersonRoot.get( "name" ) );

		factoryScope.inTransaction(session -> {
			session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
			session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "APAC" } );

			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Order.class );
			criteria.from( Order.class );
			criteria.where( criteriaBuilder.in( subquery ).value( "steve" ) );
			//noinspection removal
			var result = session.createQuery( criteria ).getResultList();
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	public void testCriteriaSubqueryWithFilters(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-subquery test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting Criteria-subquery filter tests" );

		factoryScope.inSession(session -> {
			session.enableFilter( "region" ).setParameter( "region", "APAC" );

			log.info( "Criteria query against Department with a subquery on Salesperson in the APAC reqion..." );
			var detachedCriteriaBuilder = session.getCriteriaBuilder();
			var subquery = detachedCriteriaBuilder
					.createQuery( Salesperson.class )
					.subquery( Department.class );
			var subqueryRoot = subquery.from( Salesperson.class );
			subquery.where( detachedCriteriaBuilder.equal( subqueryRoot.get( "name" ), "steve" ) );
			subquery.select( subqueryRoot.get( "department" ) );

			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Department.class );
			criteria.where( criteriaBuilder.in( criteria.from( Department.class ) ).value( subquery ) );

			//noinspection removal
			var departmentsQuery = session.createQuery( criteria );
			var departments = departmentsQuery.list();
			assertEquals( 1, departments.size(), "Incorrect department count" );

			log.info( "Criteria query against Department with a subquery on Salesperson in the FooBar reqion..." );

			session.enableFilter( "region" ).setParameter( "region", "Foobar" );
			departments = departmentsQuery.list();

			assertEquals( 0, departments.size(), "Incorrect department count" );

			log.info(
					"Criteria query against Order with a subquery for line items with a subquery on product and sold by a given sales person..." );
			session.enableFilter( "region" ).setParameter( "region", "APAC" );

			var lineItemSubquery = detachedCriteriaBuilder.createQuery()
					.subquery( LineItem.class );
			var itemRoot = lineItemSubquery.from( LineItem.class );
			var product = itemRoot.join( "product", JoinType.INNER );
			lineItemSubquery.where(
					detachedCriteriaBuilder.and(
							detachedCriteriaBuilder.ge( itemRoot.get( "quantity" ), 1L ),
							detachedCriteriaBuilder.equal( product.get( "name" ), "Acme Hair Gel" )
					)
			);
			lineItemSubquery.select( product.get( "id" ) );

			var orderCriteria = criteriaBuilder.createQuery( Order.class );
			var orderRoot = orderCriteria.from( Order.class );
			orderCriteria.where(
					criteriaBuilder.and(
							criteriaBuilder.exists( lineItemSubquery ),
							criteriaBuilder.equal( orderRoot.get( "buyer" ), "gavin" )
					)
			);

			//noinspection removal
			var orders = session.createQuery( orderCriteria ).list();
			assertEquals( 1, orders.size(), "Incorrect orders count" );

			log.info(
					"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month" );
			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

			var productSubquery = detachedCriteriaBuilder.createQuery().subquery( Long.class );
			var productRoot = productSubquery.from( Product.class );
			productSubquery.select( productRoot.get( "id" ) );
			productSubquery.where( detachedCriteriaBuilder.equal(
					productRoot.get( "name" ),
					"Acme Hair Gel"
			) );
			lineItemSubquery = detachedCriteriaBuilder.createQuery().subquery( LineItem.class );
			itemRoot = lineItemSubquery.from( LineItem.class );
			product = itemRoot.join( "product", JoinType.INNER );
			lineItemSubquery.where(
					detachedCriteriaBuilder.and(
							detachedCriteriaBuilder.ge( itemRoot.get( "quantity" ), 1L ),
							detachedCriteriaBuilder.in( product.get( "id" ) ).value( productSubquery )
					)
			);
			lineItemSubquery.select( product.get( "id" ) );

			orderCriteria = criteriaBuilder.createQuery( Order.class );
			orderRoot = orderCriteria.from( Order.class );
			orderCriteria.where(
					criteriaBuilder.and(
							criteriaBuilder.exists( lineItemSubquery ),
							criteriaBuilder.equal( orderRoot.get( "buyer" ), "gavin" )
					)
			);

			//noinspection removal
			orders = session.createQuery( orderCriteria ).list();
			assertEquals( 1, orders.size(), "Incorrect orders count" );

			log.info(
					"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of 4 months ago" );
			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			session.enableFilter( "effectiveDate" ).setParameter(
					"asOfDate",
					testData.fourMonthsAgo.getTime()
			);

			orderCriteria = criteriaBuilder.createQuery( Order.class );
			orderRoot = orderCriteria.from( Order.class );
			orderCriteria.where(
					criteriaBuilder.and(
							criteriaBuilder.exists( lineItemSubquery ),
							criteriaBuilder.equal( orderRoot.get( "buyer" ), "gavin" )
					)
			);

			//noinspection removal
			orders = session.createQuery( orderCriteria ).list();
			assertEquals( 0, orders.size(), "Incorrect orders count" );
		} );
	}

	@Test
	public void testHQLSubqueryWithFilters(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// HQL subquery with filters test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting HQL subquery with filters tests" );
		factoryScope.inSession(session -> {
			session.enableFilter( "region" ).setParameter( "region", "APAC" );

			log.info( "query against Department with a subquery on Salesperson in the APAC reqion..." );

			//noinspection deprecation
			var departments = session.createQuery(
					"select d from Department as d where d in (select s.department from Salesperson s where s.name = ?1)"
			).setParameter( 1, "steve" ).list();
			assertEquals( 1, departments.size(), "Incorrect department count" );

			log.info( "query against Department with a subquery on Salesperson in the FooBar reqion..." );

			session.enableFilter( "region" ).setParameter( "region", "Foobar" );
			//noinspection deprecation
			departments = session.createQuery(
					"select d from Department as d where d in (select s.department from Salesperson s where s.name = ?1)" )
					.setParameter( 1, "steve" )
					.list();

			assertEquals( 0, departments.size(), "Incorrect department count" );

			log.info(
					"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region for a given buyer" );
			session.enableFilter( "region" ).setParameter( "region", "APAC" );

			//noinspection deprecation
			var orders = session.createQuery(
					"select o from Order as o where exists (select li.id from LineItem li, Product as p where p.id = li.product.id and li.quantity >= ?1 and p.name = ?2) and o.buyer = ?3" )
					.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();
			assertEquals( 1, orders.size(), "Incorrect orders count" );

			log.info(
					"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month" );

			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

			//noinspection deprecation
			orders = session.createQuery(
					"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product.id in (select p.id from Product p where p.name = ?2)) and o.buyer = ?3" )
					.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();
			assertEquals( 1, orders.size(), "Incorrect orders count" );


			log.info(
					"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of 4 months ago"
			);

			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			session.enableFilter( "effectiveDate" ).setParameter(
					"asOfDate",
					testData.fourMonthsAgo.getTime()
			);

			//noinspection deprecation
			orders = session.createQuery(
					"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product in (select p from Product p where p.name = ?2)) and o.buyer = ?3" )
					.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

			assertEquals( 0, orders.size(), "Incorrect orders count" );

			log.info(
					"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month with named types" );

			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

			//noinspection deprecation
			orders = session.createQuery(
					"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product in (select p from Product p where p.name = ?2)) and o.buyer = ?3" )
					.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

			assertEquals( 1, orders.size(), "Incorrect orders count" );

			log.info(
					"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month with mixed types" );

			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

			//noinspection deprecation
			orders = session.createQuery(
					"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product in (select p from Product p where p.name = ?2)) and o.buyer = ?3" )
					.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

			assertEquals( 1, orders.size(), "Incorrect orders count" );
		} );
	}

	@Test
	@JiraKey(value = "HHH-5932")
	public void testHqlQueryWithColons(SessionFactoryScope factoryScope) {
		factoryScope.inSession(session -> {
			session.enableFilter( "region" ).setParameter( "region", "PACA" );
			//noinspection deprecation
			session.createQuery( "from Salesperson p where p.name = ':hibernate'" ).list();
		} );
	}

	@Test
	public void testFilterApplicationOnHqlQueryWithImplicitSubqueryContainingPositionalParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			var queryString = "from Order o where ?1 in ( select sp.name from Salesperson sp )";

			// first a control-group query
			//noinspection deprecation
			var result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
			assertEquals( 2, result.size() );

			// now lets enable filters on Order...
			session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
			//noinspection deprecation
			result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
			assertEquals( 1, result.size() );

			// now, lets additionally enable filter on Salesperson.  First a valid one...
			session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "APAC" } );
			//noinspection deprecation
			result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
			assertEquals( 1, result.size() );

			// ... then a silly one...
			session.enableFilter( "regionlist" ).setParameterList(
					"regions",
					new String[] { "gamma quadrant" }
			);
			//noinspection deprecation
			result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
			assertEquals( 0, result.size() );
		} );
	}

	@Test
	public void testFilterApplicationOnHqlQueryWithImplicitSubqueryContainingNamedParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			var queryString = "from Order o where :salesPersonName in ( select sp.name from Salesperson sp )";

			// first a control-group query
			//noinspection deprecation
			var result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
			assertEquals( 2, result.size() );

			// now lets enable filters on Order...
			session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
			//noinspection deprecation
			result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
			assertEquals( 1, result.size() );

			// now, lets additionally enable filter on Salesperson.  First a valid one...
			session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "APAC" } );
			//noinspection deprecation
			result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
			assertEquals( 1, result.size() );

			// ... then a silly one...
			session.enableFilter( "regionlist" ).setParameterList(
					"regions",
					new String[] { "gamma quadrant" }
			);
			//noinspection deprecation
			result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
			assertEquals( 0, result.size() );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsDmlTargetColumnQualifier.class)
	public void testFiltersOnSimpleHqlDelete(SessionFactoryScope factoryScope) {
		var sp = new Salesperson();
		var sp2 = new Salesperson();

		factoryScope.inTransaction(session -> {
			sp.setName( "steve" );
			sp.setRegion( "NA" );
			session.persist( sp );
			sp2.setName( "john" );
			sp2.setRegion( "APAC" );
			session.persist( sp2 );
		} );

		factoryScope.inTransaction(session -> {
			session.enableFilter( "region" ).setParameter( "region", "NA" );
			//noinspection deprecation
			int count = session.createQuery( "delete from Salesperson" ).executeUpdate();
			assertEquals( 1, count );
			session.remove( sp2 );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsDmlTargetColumnQualifier.class)
	public void testFiltersOnMultiTableHqlDelete(SessionFactoryScope factoryScope) {
		var sp = new Salesperson();
		var sp2 = new Salesperson();

		factoryScope.inTransaction(session -> {
			sp.setName( "steve" );
			sp.setRegion( "NA" );
			session.persist( sp );
			sp2.setName( "john" );
			sp2.setRegion( "APAC" );
			session.persist( sp2 );
		} );

		factoryScope.inTransaction(session -> {
			session.enableFilter( "region" ).setParameter( "region", "NA" );
			//noinspection deprecation
			int count = session.createQuery( "delete from Salesperson" ).executeUpdate();
			assertEquals( 1, count );
			session.remove( sp2 );
		} );
	}

	@Test
	public void testFindFilters(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Get() test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting find() filter tests (eager assoc. fetching)." );

		factoryScope.inSession(session -> {
			session.enableFilter( "region" ).setParameter( "region", "APAC" );

			log.info( "Performing find()..." );
			var salesperson = session.find( Salesperson.class, testData.steveId );
			assertNotNull( salesperson );
			assertEquals( 1, salesperson.getOrders().size(), "Incorrect order count" );
		} );
	}

	@Test
	public void testOneToManyFilters(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting one-to-many collection loader filter tests." );

		factoryScope.inSession(session -> {
			session.enableFilter( "seniorSalespersons" )
					.setParameter( "asOfDate", testData.lastMonth.getTime() );

			log.info( "Performing load of Department..." );
			var department = session.getReference( Department.class, testData.deptId );
			var salespersons = department.getSalespersons();
			assertEquals( 1, salespersons.size(), "Incorrect salesperson count" );
		} );
	}

	@Test
	public void testInStyleFilterParameter(SessionFactoryScope factoryScope) {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting one-to-many collection loader filter tests." );
		factoryScope.inSession(session -> {
			session.enableFilter( "regionlist" )
					.setParameterList( "regions", new String[] { "LA", "APAC" } );

			log.debug( "Performing query of Salespersons" );
			//noinspection deprecation
			var salespersons = session.createQuery( "from Salesperson" ).list();
			assertEquals( 1, salespersons.size(), "Incorrect salesperson count" );
		} );
	}

	@Test
	public void testManyToManyFilterOnCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inSession(session -> {
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Product.class );
			var root = criteria.from( Product.class );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), testData.prod1Id ) );

			//noinspection removal
			var prod = session.createQuery( criteria )
					.setTupleTransformer( (tuple, aliases) -> (Product) tuple[0] )
					.uniqueResult();

			assertNotNull( prod );
			assertEquals( 1, prod.getCategories().size(), "Incorrect Product.categories count for filter" );
		} );
	}

	@Test
	public void testManyToManyFilterOnLoad(SessionFactoryScope factoryScope) {
		var sessionFactory = factoryScope.getSessionFactory();
		var stats = sessionFactory.getStatistics();

		factoryScope.inSession(session -> {
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

			var prod = session.find( Product.class, testData.prod1Id );

			long initLoadCount = stats.getCollectionLoadCount();
			long initFetchCount = stats.getCollectionFetchCount();

			// should already have been initialized...
			int size = prod.getCategories().size();
			assertEquals( 1, size, "Incorrect filtered collection count" );

			long currLoadCount = stats.getCollectionLoadCount();
			long currFetchCount = stats.getCollectionFetchCount();

			assertTrue( (initLoadCount == currLoadCount ) && (initFetchCount == currFetchCount ),
					"load with join fetch of many-to-many did not trigger join fetch" );

			// make sure we did not get back a collection of proxies
			long initEntityLoadCount = stats.getEntityLoadCount();
			for ( Object o : prod.getCategories() ) {
				Category cat = (Category) o;
				log.debugf( " ===> %s", cat.getName() );
			}
			long currEntityLoadCount = stats.getEntityLoadCount();

			assertEquals( initEntityLoadCount, currEntityLoadCount,
					"load with join fetch of many-to-many did not trigger *complete* join fetch" );
		} );
	}

	@Test
	public void testManyToManyOnCollectionLoadAfterHQL(SessionFactoryScope factoryScope) {
		factoryScope.inSession(session -> {
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

			// Force the categories to not get initialized here
			//noinspection removal
			var result = session.createQuery( "from Product as p where p.id = :id", Product.class )
					.setParameter( "id", testData.prod1Id )
					.getResultList();
			assertFalse( result.isEmpty(), "No products returned from HQL" );

			var prod = result.get( 0 );
			assertNotNull( prod );
			assertEquals( 1, prod.getCategories().size(),
					"Incorrect Product.categories count for filter on collection load" );
		} );
	}

	@Test
	public void testManyToManyFilterOnQuery(SessionFactoryScope factoryScope) {
		factoryScope.inSession(session -> {
			session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

			//noinspection removal
			var result = session.createQuery(
					"from Product p inner join fetch p.categories",
					Product.class
			)
					.getResultList();
			assertFalse( result.isEmpty(), "No products returned from HQL many-to-many filter case" );

			var prod = result.get( 0 );

			assertNotNull( prod );
			assertEquals( 1, prod.getCategories().size(),
					"Incorrect Product.categories count for filter with HQL" );
		} );
	}

	@Test
	public void testManyToManyBase(SessionFactoryScope factoryScope) {
		var stats = factoryScope.getSessionFactory().getStatistics();

		factoryScope.inSession(session -> {
			var prod = session.find( Product.class, testData.prod1Id );

			long initLoadCount = stats.getCollectionLoadCount();
			long initFetchCount = stats.getCollectionFetchCount();

			// should already have been initialized...
			int size = prod.getCategories().size();
			assertEquals( 2, size, "Incorrect non-filtered collection count" );

			long currLoadCount = stats.getCollectionLoadCount();
			long currFetchCount = stats.getCollectionFetchCount();

			assertTrue( (initLoadCount == currLoadCount ) && (initFetchCount == currFetchCount ),
					"load with join fetch of many-to-many did not trigger join fetch" );

			// make sure we did not get back a collection of proxies
			long initEntityLoadCount = stats.getEntityLoadCount();
			for ( Object o : prod.getCategories() ) {
				var cat = (Category) o;
				log.debugf( " ===> %s", cat.getName() );
			}
			long currEntityLoadCount = stats.getEntityLoadCount();

			assertEquals( initEntityLoadCount, currEntityLoadCount,
					"load with join fetch of many-to-many did not trigger *complete* join fetch" );
		} );
	}

	@Test
	public void testManyToManyBaseThruCriteria(SessionFactoryScope factoryScope) {
		var stats = factoryScope.getSessionFactory().getStatistics();

		factoryScope.inSession( session -> {
			stats.clear();

			var criteriaBuilder = session.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Product.class );
			var root = criteria.from( Product.class );
			root.fetch( "categories" );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), testData.prod1Id ) );

			//noinspection removal
			var result = session.createQuery( criteria ).list();
			var prod = result.get( 0 );

			long initLoadCount = stats.getCollectionLoadCount();
			long initFetchCount = stats.getCollectionFetchCount();

			// should already have been initialized...
			int size = prod.getCategories().size();
			assertEquals( 2, size, "Incorrect non-filtered collection count" );

			long currLoadCount = stats.getCollectionLoadCount();
			long currFetchCount = stats.getCollectionFetchCount();

			assertTrue( (initLoadCount == currLoadCount ) && (initFetchCount == currFetchCount ),
					"load with join fetch of many-to-many did not trigger join fetch" );

			// make sure we did not get back a collection of proxies
			long initEntityLoadCount = stats.getEntityLoadCount();
			for ( Object o : prod.getCategories() ) {
				var cat = (Category) o;
				log.debugf( " ===> %s", cat.getName() );
			}
			long currEntityLoadCount = stats.getEntityLoadCount();

			assertEquals( initEntityLoadCount, currEntityLoadCount,
					"load with join fetch of many-to-many did not trigger *complete* join fetch" );
		} );
	}


	private static class TestData {
		private Long steveId;
		private Long deptId;
		private Long prod1Id;
		private Calendar lastMonth;
		private Calendar nextMonth;
		private Calendar sixMonthsAgo;
		private Calendar fourMonthsAgo;

		@SuppressWarnings("unchecked")
		private void prepare(SessionFactoryScope factoryScope) {
			factoryScope.inTransaction(session -> {
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

				session.persist( dept );
				deptId = dept.getId();

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

				session.persist( steve );
				session.persist( max );

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

				session.persist( product1 );
				prod1Id = product1.getId();

				Order order1 = new Order();
				order1.setBuyer( "gavin" );
				order1.setRegion( "APAC" );
				order1.setPlacementDate( sixMonthsAgo.getTime() );
				order1.setFulfillmentDate( fourMonthsAgo.getTime() );
				order1.setSalesperson( steve );
				order1.addLineItem( product1, 500 );

				session.persist( order1 );

				Product product2 = new Product();
				product2.setName( "Acme Super-Duper DTO Factory" );
				product2.setStockNumber( 124 );
				product1.setWeightPounds( 10.0 );
				product2.setEffectiveStartDate( sixMonthsAgo.getTime() );
				product2.setEffectiveEndDate( new Date() );

				Category cat3 = new Category( "test cat 2", sixMonthsAgo.getTime(), new Date() );
				product2.addCategory( cat3 );

				session.persist( product2 );

				// An uncategorized product
				Product product3 = new Product();
				product3.setName( "Uncategorized product" );
				session.persist( product3 );

				Order order2 = new Order();
				order2.setBuyer( "christian" );
				order2.setRegion( "EMEA" );
				order2.setPlacementDate( lastMonth.getTime() );
				order2.setSalesperson( steve );
				order2.addLineItem( product2, -1 );

				session.persist( order2 );
			} );
		}

		private void release(SessionFactoryScope factoryScope) {
			factoryScope.dropData();
		}
	}
}
