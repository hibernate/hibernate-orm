/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.Query;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Implementation of DynamicFilterTest.
 *
 * @author Steve Ebersole
 */
public class DynamicFilterTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] {
				"filter/defs.hbm.xml",
				"filter/LineItem.hbm.xml",
				"filter/Order.hbm.xml",
				"filter/Product.hbm.xml",
				"filter/Salesperson.hbm.xml",
				"filter/Department.hbm.xml",
				"filter/Category.hbm.xml"
		};
	}

	private TestData testData;

	@Before
	public void setTestData() {
		testData = new TestData();
		testData.prepare();
	}

	@After
	public void releaseTestData() {
		testData.release();
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Override
	public void addSettings(Map<String,Object> settings) {
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, "1" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsUnionInSubquery.class)
	public void testSqlSyntaxOfFiltersWithUnions() {
		Session session = openSession();
		session.enableFilter( "unioned" );
		session.createQuery( "from Category" ).list();
		session.close();
	}

	@Test
	public void testSecondLevelCachedCollectionsFiltering() {

		CollectionPersister persister = sessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor(Salesperson.class.getName() + ".orders");
		CollectionDataAccess cache = persister.getCacheAccessStrategy();

		CollectionCacheEntry cachedData = fromSession(
				session -> {
					// Force a collection into the second level cache, with its non-filtered elements
					Salesperson sp = session.getReference( Salesperson.class, testData.steveId );
					Hibernate.initialize( sp.getOrders() );
					assertTrue( "No cache for collection", persister.hasCache() );
					Object cacheKey = cache.generateCacheKey(
							testData.steveId,
							persister,
							sessionFactory(),
							session.getTenantIdentifier()
					);
					CollectionCacheEntry cached = (CollectionCacheEntry) cache.get(
							session,
							cacheKey
					);
					assertNotNull( "collection was not in cache", cached );
					return cached;
				}
		);

		inSession(
				session -> {
					session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
					Salesperson sp = (Salesperson) session.createQuery( "from Salesperson as s where s.id = :id" )
							.setParameter( "id", testData.steveId )
							.uniqueResult();
					assertEquals( "Filtered-collection not bypassing 2L-cache", 1, sp.getOrders().size() );

					Object cacheKey2 = cache.generateCacheKey(
							testData.steveId,
							persister,
							sessionFactory(),
							session.getTenantIdentifier()
					);
					CollectionCacheEntry cachedData2 = (CollectionCacheEntry) persister.getCacheAccessStrategy()
							.get( session, cacheKey2 );
					assertNotNull( "collection no longer in cache!", cachedData2 );
					assertSame( "Different cache values!", cachedData, cachedData2 );
				}
		);

		inSession(
				session -> {
					session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
					Salesperson sp = session.getReference( Salesperson.class, testData.steveId );
					assertEquals( "Filtered-collection not bypassing 2L-cache", 1, sp.getOrders().size() );
				}
		);

		// Finally, make sure that the original cached version did not get over-written
		inSession(
				session -> {
					Salesperson sp = session.getReference( Salesperson.class, testData.steveId );
					assertEquals( "Actual cached version got over-written", 2, sp.getOrders().size() );
				}
		);
	}

	@Test
	public void testCombinedClassAndCollectionFiltersEnabled() {

		inSession(
				session -> {
					session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "LA", "APAC" } );
					session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

					// test retrieval through hql with the collection as non-eager
					List<Salesperson> salespersons = session.createQuery(
							"select s from Salesperson as s",
							Salesperson.class
					)
							.getResultList();
					assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
					Salesperson sp = salespersons.get( 0 );
					assertEquals( "Incorrect order count", 1, sp.getOrders().size() );

					session.clear();

					session.disableFilter( "regionlist" );
					session.enableFilter( "regionlist" ).setParameterList(
							"regions",
							new String[] { "LA", "APAC", "APAC" }
					);
					// Second test retrieval through hql with the collection as non-eager with different region list
					salespersons = session.createQuery( "select s from Salesperson as s", Salesperson.class )
							.getResultList();
					assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
					sp = salespersons.get( 0 );
					assertEquals( "Incorrect order count", 1, sp.getOrders().size() );

					session.clear();

					// test retrieval through hql with the collection join fetched
					salespersons = session.createQuery(
							"select s from Salesperson as s left join fetch s.orders",
							Salesperson.class
					).getResultList();
					assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
					sp = salespersons.get( 0 );
					assertEquals( "Incorrect order count", 1, sp.getOrders().size() );
				}
		);
	}

	@Test
	public void testHqlFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// HQL test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting HQL filter tests" );

		inSession(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "APAC" );

					session.enableFilter( "effectiveDate" )
							.setParameter( "asOfDate", testData.lastMonth.getTime() );

					log.info( "HQL against Salesperson..." );
					List results = session.createQuery( "select s from Salesperson as s left join fetch s.orders" )
							.list();
					assertTrue( "Incorrect filtered HQL result count [" + results.size() + "]", results.size() == 1 );
					Salesperson result = (Salesperson) results.get( 0 );
					assertTrue( "Incorrect collectionfilter count", result.getOrders().size() == 1 );

					log.info( "HQL against Product..." );
					results = session.createQuery( "from Product as p where p.stockNumber = ?1" )
							.setParameter( 1, 124 )
							.list();
					assertTrue( results.size() == 1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-14567")
	public void testHqlFiltersAppliedAfterQueryCreation() {
		inTransaction( session -> {
			Query<Salesperson> query = session.createQuery(
					"select s from Salesperson s",
					Salesperson.class
			);
			List<Salesperson> list = query.list();
			assertThat( list ).hasSize( 2 );

			session.enableFilter( "region" ).setParameter( "region", "APAC" );
			assertThat( query.list() ).hasSize( 1 );
		} );
	}

	@Test
	public void testFiltersWithCustomerReadAndWrite() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Custom SQL read/write with filter
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting HQL filter with custom SQL get/set tests" );

		inSession(
				session -> {
					session.enableFilter( "heavyProducts" ).setParameter( "weightKilograms", 4d );
					log.info( "HQL against Product..." );
					List<Product> results = session.createQuery( "from Product", Product.class ).getResultList();
					assertEquals( 1, results.size() );
				}
		);
	}

	@Test
	public void testCriteriaQueryFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-query test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting Criteria-query filter tests" );

		inSession(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "APAC" );

					session.enableFilter( "fulfilledOrders" )
							.setParameter( "asOfDate", testData.lastMonth.getTime() );

					session.enableFilter( "effectiveDate" )
							.setParameter( "asOfDate", testData.lastMonth.getTime() );

					log.info( "Criteria query against Salesperson..." );
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Salesperson> criteria = criteriaBuilder.createQuery( Salesperson.class );
					Root<Salesperson> from = criteria.from( Salesperson.class );
					from.fetch( "orders", JoinType.LEFT );
					List<Salesperson> salespersons = session.createQuery( criteria ).getResultList();
//		List salespersons = session.createCriteria( Salesperson.class )
//		        .setFetchMode( "orders", FetchMode.JOIN )
//		        .list();
					assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
					assertEquals( "Incorrect order count", 1, ( salespersons.get( 0 ) ).getOrders().size() );

					log.info( "Criteria query against Product..." );
					CriteriaQuery<Product> productCriteria = criteriaBuilder.createQuery( Product.class );
					Root<Product> productRoot = productCriteria.from( Product.class );
					productCriteria.where( criteriaBuilder.equal( productRoot.get( "stockNumber" ), 124 ) );

					List<Product> products = session.createQuery( productCriteria ).getResultList();
//		List products = session.createCriteria( Product.class )
//		        .add( Restrictions.eq( "stockNumber", 124 ) )
//		        .list();
					assertEquals( "Incorrect product count", 1, products.size() );

				}
		);
	}

	@Test
	public void testCriteriaControl() {

		// the subquery...
//		DetachedCriteria subquery = DetachedCriteria.forClass( Salesperson.class )
//				.setProjection( Property.forName( "name" ) );
		CriteriaBuilder detachedCriteriaBuilder = sessionFactory().getCriteriaBuilder();
		CriteriaQuery<Salesperson> query = detachedCriteriaBuilder.createQuery( Salesperson.class );
		Subquery<String> subquery = query.subquery( String.class );
		Root<Salesperson> salespersonRoot = subquery.from( Salesperson.class );
		subquery.select( salespersonRoot.get( "name" ) );

		inTransaction(
				session -> {
					session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
					session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "APAC" } );

					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Order> criteria = criteriaBuilder.createQuery( Order.class );
					criteria.from( Order.class );
					criteria.where( criteriaBuilder.in( subquery ).value( "steve" ) );
					List<Order> result = session.createQuery( criteria ).getResultList();

//					List result = session.createCriteria( Order.class )
//							.add( Subqueries.in( "steve", subquery ) )
//							.list();
					assertEquals( 1, result.size() );
				}
		);
	}

	@Test
	public void testCriteriaSubqueryWithFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-subquery test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting Criteria-subquery filter tests" );

		inSession(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "APAC" );

					log.info( "Criteria query against Department with a subquery on Salesperson in the APAC reqion..." );
//		DetachedCriteria salespersonSubquery = DetachedCriteria.forClass(Salesperson.class)
//				.add(Restrictions.eq("name", "steve"))
//				.setProjection(Property.forName("department"));
					CriteriaBuilder detachedCriteriaBuilder = sessionFactory().getCriteriaBuilder();
					Subquery<Department> subquery = detachedCriteriaBuilder.createQuery( Salesperson.class )
							.subquery( Department.class );
					Root<Salesperson> subqueryRoot = subquery.from( Salesperson.class );
					subquery.where( detachedCriteriaBuilder.equal( subqueryRoot.get( "name" ), "steve" ) );
					subquery.select( subqueryRoot.get( "department" ) );

					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Department> criteria = criteriaBuilder.createQuery( Department.class );
					criteria.where( criteriaBuilder.in( criteria.from( Department.class ) ).value( subquery ) );

					Query<Department> departmentsQuery = session.createQuery( criteria );
					List<Department> departments = departmentsQuery.list();

//		Criteria departmentsQuery = session.createCriteria(Department.class).add(Subqueries.propertyIn("id", salespersonSubquery));
//		List departments = departmentsQuery.list();

					assertEquals( "Incorrect department count", 1, departments.size() );

					log.info( "Criteria query against Department with a subquery on Salesperson in the FooBar reqion..." );

					session.enableFilter( "region" ).setParameter( "region", "Foobar" );
					departments = departmentsQuery.list();

					assertEquals( "Incorrect department count", 0, departments.size() );

					log.info(
							"Criteria query against Order with a subquery for line items with a subquery on product and sold by a given sales person..." );
					session.enableFilter( "region" ).setParameter( "region", "APAC" );

					Subquery<LineItem> lineItemSubquery = detachedCriteriaBuilder.createQuery()
							.subquery( LineItem.class );
					Root<LineItem> itemRoot = lineItemSubquery.from( LineItem.class );
					Join<Object, Object> product = itemRoot.join( "product", JoinType.INNER );
					lineItemSubquery.where(
							detachedCriteriaBuilder.and(
									detachedCriteriaBuilder.ge( itemRoot.get( "quantity" ), 1L ),
									detachedCriteriaBuilder.equal( product.get( "name" ), "Acme Hair Gel" )
							)
					);
					lineItemSubquery.select( product.get( "id" ) );
//		DetachedCriteria lineItemSubquery = DetachedCriteria.forClass(LineItem.class)
//				.add( Restrictions.ge( "quantity", 1L ) )
//				.createCriteria( "product" )
//				.add( Restrictions.eq( "name", "Acme Hair Gel" ) )
//				.setProjection( Property.forName( "id" ) );

					CriteriaQuery<Order> orderCriteria = criteriaBuilder.createQuery( Order.class );
					Root<Order> orderRoot = orderCriteria.from( Order.class );
					orderCriteria.where(
							criteriaBuilder.and(
									criteriaBuilder.exists( lineItemSubquery ),
									criteriaBuilder.equal( orderRoot.get( "buyer" ), "gavin" )
							)
					);

					List<Order> orders = session.createQuery( orderCriteria ).list();

//		List orders = session.createCriteria(Order.class)
//				.add(Subqueries.exists(lineItemSubquery))
//				.add(Restrictions.eq("buyer", "gavin"))
//				.list();

					assertEquals( "Incorrect orders count", 1, orders.size() );

					log.info(
							"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month" );
					session.enableFilter( "region" ).setParameter( "region", "APAC" );
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

					Subquery<Long> productSubquery = detachedCriteriaBuilder.createQuery().subquery( Long.class );
					Root<Product> productRoot = productSubquery.from( Product.class );
					productSubquery.select( productRoot.get( "id" ) );
					productSubquery.where( detachedCriteriaBuilder.equal(
							productRoot.get( "name" ),
							"Acme Hair Gel"
					) );
//		DetachedCriteria productSubquery = DetachedCriteria.forClass(Product.class)
//				.add(Restrictions.eq("name", "Acme Hair Gel"))
//				.setProjection(Property.forName("id"));

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

					orders = session.createQuery( orderCriteria ).list();
//		lineItemSubquery = DetachedCriteria.forClass(LineItem.class)
//				.add(Restrictions.ge("quantity", 1L ))
//				.createCriteria("product")
//				.add(Subqueries.propertyIn("id", productSubquery))
//				.setProjection(Property.forName("id"));
//
//		orders = session.createCriteria(Order.class)
//				.add(Subqueries.exists(lineItemSubquery))
//				.add(Restrictions.eq("buyer", "gavin"))
//				.list();

					assertEquals( "Incorrect orders count", 1, orders.size() );


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

					orders = session.createQuery( orderCriteria ).list();
//		orders = session.createCriteria(Order.class)
//				.add(Subqueries.exists(lineItemSubquery))
//				.add(Restrictions.eq("buyer", "gavin"))
//				.list();

					assertEquals( "Incorrect orders count", 0, orders.size() );

				}
		);

	}

	@Test
	public void testHQLSubqueryWithFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// HQL subquery with filters test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting HQL subquery with filters tests" );
		inSession(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "APAC" );

					log.info( "query against Department with a subquery on Salesperson in the APAC reqion..." );

					List departments = session.createQuery(
							"select d from Department as d where d in (select s.department from Salesperson s where s.name = ?1)"
					).setParameter( 1, "steve" ).list();

					assertEquals( "Incorrect department count", 1, departments.size() );

					log.info( "query against Department with a subquery on Salesperson in the FooBar reqion..." );

					session.enableFilter( "region" ).setParameter( "region", "Foobar" );
					departments = session.createQuery(
							"select d from Department as d where d in (select s.department from Salesperson s where s.name = ?1)" )
							.setParameter( 1, "steve" )
							.list();

					assertEquals( "Incorrect department count", 0, departments.size() );

					log.info(
							"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region for a given buyer" );
					session.enableFilter( "region" ).setParameter( "region", "APAC" );

					List orders = session.createQuery(
							"select o from Order as o where exists (select li.id from LineItem li, Product as p where p.id = li.product.id and li.quantity >= ?1 and p.name = ?2) and o.buyer = ?3" )
							.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

					assertEquals( "Incorrect orders count", 1, orders.size() );

					log.info(
							"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month" );

					session.enableFilter( "region" ).setParameter( "region", "APAC" );
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

					orders = session.createQuery(
							"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product.id in (select p.id from Product p where p.name = ?2)) and o.buyer = ?3" )
							.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

					assertEquals( "Incorrect orders count", 1, orders.size() );


					log.info(
							"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of 4 months ago"
					);

					session.enableFilter( "region" ).setParameter( "region", "APAC" );
					session.enableFilter( "effectiveDate" ).setParameter(
							"asOfDate",
							testData.fourMonthsAgo.getTime()
					);

					orders = session.createQuery(
							"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product in (select p from Product p where p.name = ?2)) and o.buyer = ?3" )
							.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

					assertEquals( "Incorrect orders count", 0, orders.size() );

					log.info(
							"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month with named types" );

					session.enableFilter( "region" ).setParameter( "region", "APAC" );
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

					orders = session.createQuery(
							"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product in (select p from Product p where p.name = ?2)) and o.buyer = ?3" )
							.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

					assertEquals( "Incorrect orders count", 1, orders.size() );

					log.info(
							"query against Order with a subquery for line items with a subquery line items where the product name is Acme Hair Gel and the quantity is greater than 1 in a given region and the product is effective as of last month with mixed types" );

					session.enableFilter( "region" ).setParameter( "region", "APAC" );
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

					orders = session.createQuery(
							"select o from Order as o where exists (select li.id from LineItem li where li.quantity >= ?1 and li.product in (select p from Product p where p.name = ?2)) and o.buyer = ?3" )
							.setParameter( 1, 1L ).setParameter( 2, "Acme Hair Gel" ).setParameter( 3, "gavin" ).list();

					assertEquals( "Incorrect orders count", 1, orders.size() );

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5932")
	public void testHqlQueryWithColons() {
		inSession(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "PACA" );
					session.createQuery( "from Salesperson p where p.name = ':hibernate'" ).list();

				}
		);
	}

	@Test
	public void testFilterApplicationOnHqlQueryWithImplicitSubqueryContainingPositionalParameter() {
		inTransaction(
				session -> {
					final String queryString = "from Order o where ?1 in ( select sp.name from Salesperson sp )";

					// first a control-group query
					List result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
					assertEquals( 2, result.size() );

					// now lets enable filters on Order...
					session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
					result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
					assertEquals( 1, result.size() );

					// now, lets additionally enable filter on Salesperson.  First a valid one...
					session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "APAC" } );
					result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
					assertEquals( 1, result.size() );

					// ... then a silly one...
					session.enableFilter( "regionlist" ).setParameterList(
							"regions",
							new String[] { "gamma quadrant" }
					);
					result = session.createQuery( queryString ).setParameter( 1, "steve" ).list();
					assertEquals( 0, result.size() );
				}
		);
	}

	@Test
	public void testFilterApplicationOnHqlQueryWithImplicitSubqueryContainingNamedParameter() {
		inTransaction(
				session -> {
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
					session.enableFilter( "regionlist" ).setParameterList(
							"regions",
							new String[] { "gamma quadrant" }
					);
					result = session.createQuery( queryString ).setParameter( "salesPersonName", "steve" ).list();
					assertEquals( 0, result.size() );

				}
		);
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsDmlTargetColumnQualifier.class)
	public void testFiltersOnSimpleHqlDelete() {
		Salesperson sp = new Salesperson();
		Salesperson sp2 = new Salesperson();
		inTransaction(
				session -> {
					sp.setName( "steve" );
					sp.setRegion( "NA" );
					session.persist( sp );
					sp2.setName( "john" );
					sp2.setRegion( "APAC" );
					session.persist( sp2 );
				}
		);

		inTransaction(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "NA" );
					int count = session.createQuery( "delete from Salesperson" ).executeUpdate();
					assertEquals( 1, count );
					session.remove( sp2 );
				}
		);
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsDmlTargetColumnQualifier.class)
	public void testFiltersOnMultiTableHqlDelete() {
		Salesperson sp = new Salesperson();
		Salesperson sp2 = new Salesperson();
		inTransaction(
				session -> {
					sp.setName( "steve" );
					sp.setRegion( "NA" );
					session.persist( sp );
					sp2.setName( "john" );
					sp2.setRegion( "APAC" );
					session.persist( sp2 );
				}
		);

		inTransaction(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "NA" );
					int count = session.createQuery( "delete from Salesperson" ).executeUpdate();
					assertEquals( 1, count );
					session.remove( sp2 );
				}
		);
	}

	@Test
	public void testGetFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Get() test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting get() filter tests (eager assoc. fetching)." );

		inSession(
				session -> {
					session.enableFilter( "region" ).setParameter( "region", "APAC" );

					log.info( "Performing get()..." );
					Salesperson salesperson = session.get( Salesperson.class, testData.steveId );
					assertNotNull( salesperson );
					assertEquals( "Incorrect order count", 1, salesperson.getOrders().size() );

				}
		);
	}

	@Test
	public void testOneToManyFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting one-to-many collection loader filter tests." );
		inSession(
				session -> {
					session.enableFilter( "seniorSalespersons" )
							.setParameter( "asOfDate", testData.lastMonth.getTime() );

					log.info( "Performing load of Department..." );
					Department department = session.getReference( Department.class, testData.deptId );
					Set salespersons = department.getSalespersons();
					assertEquals( "Incorrect salesperson count", 1, salespersons.size() );

				}
		);
	}

	@Test
	public void testInStyleFilterParameter() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info( "Starting one-to-many collection loader filter tests." );
		inSession(
				session -> {
					session.enableFilter( "regionlist" )
							.setParameterList( "regions", new String[] { "LA", "APAC" } );

					log.debug( "Performing query of Salespersons" );
					List salespersons = session.createQuery( "from Salesperson" ).list();
					assertEquals( "Incorrect salesperson count", 1, salespersons.size() );

				}
		);
	}

	@Test
	public void testManyToManyFilterOnCriteria() {
		inSession(
				session -> {
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Product> criteria = criteriaBuilder.createQuery( Product.class );
					Root<Product> root = criteria.from( Product.class );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), testData.prod1Id ) );

					Product prod = session.createQuery( criteria )
							.setTupleTransformer( (tuple, aliases) -> (Product) tuple[0] )
							.uniqueResult();

					assertNotNull( prod );
					assertEquals( "Incorrect Product.categories count for filter", 1, prod.getCategories().size() );
				}
		);
	}

	@Test
	public void testManyToManyFilterOnLoad() {
		inSession(
				session -> {
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

					Product prod = session.get( Product.class, testData.prod1Id );

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
					for ( Object o : prod.getCategories() ) {
						Category cat = (Category) o;
						System.out.println( " ===> " + cat.getName() );
					}
					long currEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();

					assertTrue(
							"load with join fetch of many-to-many did not trigger *complete* join fetch",
							( initEntityLoadCount == currEntityLoadCount )
					);
				}
		);
	}

	@Test
	public void testManyToManyOnCollectionLoadAfterHQL() {
		inSession(
				session -> {
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

					// Force the categories to not get initialized here
					List<Product> result = session.createQuery( "from Product as p where p.id = :id", Product.class )
							.setParameter( "id", testData.prod1Id )
							.getResultList();
					assertFalse( "No products returned from HQL", result.isEmpty() );

					Product prod = result.get( 0 );
					assertNotNull( prod );
					assertEquals(
							"Incorrect Product.categories count for filter on collection load",
							1,
							prod.getCategories().size()
					);
				}
		);
	}

	@Test
	public void testManyToManyFilterOnQuery() {
		inSession(
				session -> {
					session.enableFilter( "effectiveDate" ).setParameter( "asOfDate", new Date() );

					List<Product> result = session.createQuery(
							"from Product p inner join fetch p.categories",
							Product.class
					)
							.getResultList();
					assertFalse( "No products returned from HQL many-to-many filter case", result.isEmpty() );

					Product prod = result.get( 0 );

					assertNotNull( prod );
					assertEquals(
							"Incorrect Product.categories count for filter with HQL",
							1,
							prod.getCategories().size()
					);
				}
		);
	}

	@Test
	public void testManyToManyBase() {
		inSession(
				session -> {
					Product prod = session.get( Product.class, testData.prod1Id );

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
					for ( Object o : prod.getCategories() ) {
						Category cat = (Category) o;
						System.out.println( " ===> " + cat.getName() );
					}
					long currEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();

					assertTrue(
							"load with join fetch of many-to-many did not trigger *complete* join fetch",
							( initEntityLoadCount == currEntityLoadCount )
					);
				}
		);
	}

	@Test
	public void testManyToManyBaseThruCriteria() {
		inSession(
				session -> {
					sessionFactory().getStatistics().clear();
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Product> criteria = criteriaBuilder.createQuery( Product.class );
					Root<Product> root = criteria.from( Product.class );
					root.fetch( "categories" );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), testData.prod1Id ) );

					List<Product> result = session.createQuery( criteria ).list();

//		List result = session.createCriteria( Product.class )
//		        .add( Restrictions.eq( "id", testData.prod1Id ) )
//		        .list();

					Product prod = result.get( 0 );

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
					for ( Object o : prod.getCategories() ) {
						Category cat = (Category) o;
						System.out.println( " ===> " + cat.getName() );
					}
					long currEntityLoadCount = sessionFactory().getStatistics().getEntityLoadCount();

					assertTrue(
							"load with join fetch of many-to-many did not trigger *complete* join fetch",
							( initEntityLoadCount == currEntityLoadCount )
					);
				}
		);
	}


	private class TestData {
		private Long steveId;
		private Long deptId;
		private Long prod1Id;
		private Calendar lastMonth;
		private Calendar nextMonth;
		private Calendar sixMonthsAgo;
		private Calendar fourMonthsAgo;

		private final List<Object> entitiesToCleanUp = new ArrayList<>();

		private void prepare() {
			inTransaction(
					session -> {
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

						session.persist( steve );
						session.persist( max );
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

						session.persist( product1 );
						entitiesToCleanUp.add( product1 );
						prod1Id = product1.getId();

						Order order1 = new Order();
						order1.setBuyer( "gavin" );
						order1.setRegion( "APAC" );
						order1.setPlacementDate( sixMonthsAgo.getTime() );
						order1.setFulfillmentDate( fourMonthsAgo.getTime() );
						order1.setSalesperson( steve );
						order1.addLineItem( product1, 500 );

						session.persist( order1 );
						entitiesToCleanUp.add( order1 );

						Product product2 = new Product();
						product2.setName( "Acme Super-Duper DTO Factory" );
						product2.setStockNumber( 124 );
						product1.setWeightPounds( 10.0 );
						product2.setEffectiveStartDate( sixMonthsAgo.getTime() );
						product2.setEffectiveEndDate( new Date() );

						Category cat3 = new Category( "test cat 2", sixMonthsAgo.getTime(), new Date() );
						product2.addCategory( cat3 );

						session.persist( product2 );
						entitiesToCleanUp.add( product2 );

						// An uncategorized product
						Product product3 = new Product();
						product3.setName( "Uncategorized product" );
						session.persist( product3 );
						entitiesToCleanUp.add( product3 );

						Order order2 = new Order();
						order2.setBuyer( "christian" );
						order2.setRegion( "EMEA" );
						order2.setPlacementDate( lastMonth.getTime() );
						order2.setSalesperson( steve );
						order2.addLineItem( product2, -1 );

						session.persist( order2 );
						entitiesToCleanUp.add( order2 );
					}
			);
		}

		private void release() {
			inTransaction(
					session -> {
						for ( Object o : entitiesToCleanUp ) {
							session.remove( o );
						}
					}
			);
		}
	}
}
