/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.SessionFactory;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.orm.test.jpa.Wallet;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 * @author Chris Cranford
 * @author Yanming Zhou
 */
@Jpa(
		annotatedClasses = {
				Item.class,
				Distributor.class,
				Wallet.class,
				Employee.class,
				Contractor.class
		},
		generateStatistics = true
)
public class QueryTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-7192")
	public void testTypedManipulationQueryError(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.createQuery( "delete Item", Item.class )
			);
			assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.createQuery( "update Item i set i.name = 'someName'", Item.class )
			);
		} );
	}

	@Test
	public void testPagedQuery(EntityManagerFactoryScope scope)  {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			item = new Item( "Computer", "Apple II" );
			entityManager.persist( item );
			Query q = entityManager.createQuery( "select i from " + Item.class.getName() + " i where i.name like :itemName", Item.class );
			q.setParameter( "itemName", "%" );
			q.setMaxResults( 1 );
			q.getSingleResult();
			q = entityManager.createQuery( "select i from Item i where i.name like :itemName", Item.class );
			q.setParameter( "itemName", "%" );
			q.setFirstResult( 1 );
			q.setMaxResults( 1 );
		} );
	}

	@Test
	public void testNullPositionalParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			TypedQuery<Item> q = entityManager.createQuery( "from Item i where i.intVal=?1", Item.class );
			q.setParameter( 1, null );
			List<Item> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null and ?1 is null", Item.class );
			q.setParameter( 1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null or i.intVal = ?1", Item.class );
			q.setParameter( 1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testNullPositionalParameterParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			TypedQuery<Item> q = entityManager.createQuery( "from Item i where i.intVal=?1", Item.class );
			Parameter<Integer> p = new Parameter<>() {
				@Override
				public String getName() {
					return null;
				}

				@Override
				public Integer getPosition() {
					return 1;
				}

				@Override
				public Class<Integer> getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			List<Item> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null and ?1 is null", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null or i.intVal = ?1", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testNullPositionalParameterParameterIncompatible(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			TypedQuery<Item> q = entityManager.createQuery( "from Item i where i.intVal=?1", Item.class );
			Parameter<Long> p = new Parameter<>() {
				@Override
				public String getName() {
					return null;
				}

				@Override
				public Integer getPosition() {
					return 1;
				}

				@Override
				public Class<Long> getParameterType() {
					return Long.class;
				}
			};
			q.setParameter( p, null );
			List<Item> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null and ?1 is null", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null or i.intVal = ?1", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testNullNamedParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			TypedQuery<Item> q = entityManager.createQuery( "from Item i where i.intVal=:iVal", Item.class );
			q.setParameter( "iVal", null );
			List<Item> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null and :iVal is null", Item.class );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null or i.intVal = :iVal", Item.class );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testNullNamedParameterParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			TypedQuery<Item> q = entityManager.createQuery( "from Item i where i.intVal=:iVal", Item.class );
			Parameter<Integer> p = new Parameter<>() {
				@Override
				public String getName() {
					return "iVal";
				}

				@Override
				public Integer getPosition() {
					return null;
				}

				@Override
				public Class<Integer> getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			List<Item> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null and :iVal is null", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null or i.intVal = :iVal", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testNullNamedParameterParameterIncompatible(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			TypedQuery<Item> q = entityManager.createQuery( "from Item i where i.intVal=:iVal", Item.class );
			Parameter<Long> p = new Parameter<>() {
				@Override
				public String getName() {
					return "iVal";
				}

				@Override
				public Integer getPosition() {
					return null;
				}

				@Override
				public Class<Long> getParameterType() {
					return Long.class;
				}
			};
			q.setParameter( p, null );
			List<Item> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null and :iVal is null", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createQuery( "from Item i where i.intVal is null or i.intVal = :iVal", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = GaussDBDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = CockroachDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = InformixDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullPositionalParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			// native queries don't seem to flush by default ?!?
			entityManager.flush();

			Query q = entityManager.createNativeQuery( "select * from Item i where i.int_val=?", Item.class );
			q.setParameter( 1, null );
			List<?> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where i.int_val is null and ? is null", Item.class );
			q.setParameter( 1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = ?", Item.class );
			q.setParameter(1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10161")
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = GaussDBDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = CockroachDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = InformixDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullPositionalParameterParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			// native queries don't seem to flush by default ?!?
			entityManager.flush();

			Query q = entityManager.createNativeQuery( "select * from Item i where i.int_val=?", Item.class );
			Parameter<Integer> p = new Parameter<>() {
				@Override
				public String getName() {
					return null;
				}

				@Override
				public Integer getPosition() {
					return 1;
				}

				@Override
				public Class<Integer> getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			List<?> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where i.int_val is null and ? is null", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = ?", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = GaussDBDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = CockroachDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = InformixDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullNamedParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			// native queries don't seem to flush by default ?!?
			entityManager.flush();

			Query q = entityManager.createNativeQuery( "select * from Item i where i.int_val=:iVal", Item.class );
			q.setParameter( "iVal", null );
			List<?> results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where (i.int_val is null) and (:iVal is null)", Item.class );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = :iVal", Item.class );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10161")
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = GaussDBDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = CockroachDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(dialectClass = InformixDialect.class, matchSubTypes = true, reason = "HHH-10312: Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullNamedParameterParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			// native queries don't seem to flush by default ?!?
			entityManager.flush();

			Query q = entityManager.createNativeQuery( "select * from Item i where i.int_val=:iVal", Item.class );
			Parameter<Integer> p = new Parameter<>() {
				@Override
				public String getName() {
					return "iVal";
				}

				@Override
				public Integer getPosition() {
					return null;
				}

				@Override
				public Class<Integer> getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			List<?> results = q.getResultList();
			assertEquals( 0, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where (i.int_val is null) and (:iVal is null)", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = entityManager.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = :iVal", Item.class );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@JiraKey("HHH-18033")
	public void testQueryContainsQuotedSemicolonWithLimit(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			entityManager.persist( new Item( "Mouse;", "Micro$oft mouse" ) );

			TypedQuery<Item> q = entityManager.createQuery( "from Item where name like '%;%'", Item.class ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );

			q = entityManager.createQuery( "from Item where name like '%;%' ", Item.class ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );
		} );
	}

	@Test
	@JiraKey("HHH-18033")
	public void testNativeQueryContainsQuotedSemicolonWithLimit(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			entityManager.persist( new Item( "Mouse;", "Micro$oft mouse" ) );

			Query q = entityManager.createNativeQuery( "select * from Item where name like '%;%'", Item.class ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );

			q = entityManager.createNativeQuery( "select * from Item where name like '%;%' ", Item.class ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );
		} );
	}

	@Test
	@JiraKey("HHH-18033")
	@SkipForDialect(dialectClass = OracleDialect.class, matchSubTypes = true, reason = "Doesn't support semicolon as ending of statement")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Doesn't support semicolon as ending of statement")
	@SkipForDialect(dialectClass = DerbyDialect.class, matchSubTypes = true, reason = "Doesn't support semicolon as ending of statement")
	@SkipForDialect(dialectClass = DB2Dialect.class, matchSubTypes = true, reason = "Doesn't support semicolon as ending of statement")
	public void testNativeQueryContainsQuotedSemicolonAndEndsWithSemicolonWithLimit(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			entityManager.persist( new Item( "Mouse;", "Micro$oft mouse" ) );

			Query q = entityManager.createNativeQuery( "select * from Item where name like '%;%';", Item.class ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );

			q = entityManager.createNativeQuery( "select * from Item where name like '%;%' ; ", Item.class ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );
		} );
	}

	@Test
	public void testAggregationReturnType(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			item = new Item( "Computer", "Apple II" );
			entityManager.persist( item );
			Query q = entityManager.createQuery( "select count(i) from Item i where i.name like :itemName" );
			q.setParameter( "itemName", "%" );
			assertInstanceOf( Long.class, q.getSingleResult() );
		} );
	}

	@Test
	public void testTypeExpression(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Employee employee = new Employee( "Lukasz", 100.0 );
			entityManager.persist( employee );
			final Contractor contractor = new Contractor( "Kinga", 100.0, "Microsoft" );
			entityManager.persist( contractor );
			final TypedQuery<Employee> q = entityManager.createQuery( "SELECT e FROM Employee e where TYPE(e) <> Contractor", Employee.class );
			final List<Employee> result = q.getResultList();
			assertNotNull( result );
			assertEquals( List.of( employee ), result );
		} );
	}

	@Test
	@JiraKey(value = "HHH_7407")
	public void testMultipleParameterLists(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		scope.inTransaction( entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			List<String> names = List.of( item.getName() );
			TypedQuery<Item> q = entityManager.createQuery(
					"select item from Item item where item.name in :names or item.name in :names2", Item.class );
			q.setParameter( "names", names );
			q.setParameter( "names2", names );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );

			List<String> descrs = List.of( item.getDescr() );
			q = entityManager.createQuery(
					"select item from Item item where item.name in :names and ( item.descr is null or item.descr in :descrs )",
					Item.class );
			q.setParameter( "names", names );
			q.setParameter( "descrs", descrs );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH_8949")
	public void testCacheStoreAndRetrieveModeParameter(EntityManagerFactoryScope scope)  {
		scope.inTransaction( entityManager -> {

			Query query = entityManager.createQuery( "select item from Item item", Item.class );

			query.getHints().clear();

			query.setHint( "jakarta.persistence.cache.retrieveMode", CacheRetrieveMode.USE );
			query.setHint( "jakarta.persistence.cache.storeMode", CacheStoreMode.REFRESH );

			assertEquals( CacheRetrieveMode.USE, query.getHints().get( "jakarta.persistence.cache.retrieveMode" ) );
			assertEquals( CacheStoreMode.REFRESH, query.getHints().get( "jakarta.persistence.cache.storeMode" ) );

			query.getHints().clear();

			query.setHint( "jakarta.persistence.cache.retrieveMode", "USE" );
			query.setHint( "jakarta.persistence.cache.storeMode", "REFRESH" );

			assertEquals( CacheRetrieveMode.USE, query.getHints().get( "jakarta.persistence.cache.retrieveMode" ) );
			assertEquals( CacheStoreMode.REFRESH, query.getHints().get( "jakarta.persistence.cache.storeMode" ) );

		} );
	}

	@Test
	public void testJpaPositionalParameters(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			Query query = entityManager.createQuery( "from Item item where item.name =?1 or item.descr = ?1", Item.class );
			Parameter<?> p1 = query.getParameter( 1 );
			assertNotNull( p1 );
			assertNotNull( p1.getPosition() );
			assertNull( p1.getName() );

		} );
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testParameterCollectionAndPositional(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Microsoft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );
		scope.inTransaction( entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Item> q = entityManager.createQuery( "select item from Item item where item.name in ?1 and item.descr = ?2", Item.class );
			List<String> params = new ArrayList<>();
			params.add( item.getName() );
			params.add( item2.getName() );
			q.setParameter( 1, params );
			q.setParameter( 2, item2.getDescr() );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testParameterCollectionParenthesesAndPositional(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Microsoft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		scope.inTransaction( entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Item> q = entityManager.createQuery(
					"select item from Item item where item.name in (?1) and item.descr = ?2", Item.class );
			List<String> params = new ArrayList<>();
			params.add( item.getName() );
			params.add( item2.getName() );
			q.setParameter( 1, params );
			q.setParameter( 2, item2.getDescr() );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testParameterCollectionSingletonParenthesesAndPositional(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Microsoft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		scope.inTransaction( entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Item> q = entityManager.createQuery(
					"select item from Item item where item.name in (?1) and item.descr = ?2", Item.class );
			List<String> params = new ArrayList<>();
			params.add( item2.getName() );
			q.setParameter( 1, params );
			q.setParameter( 2, item2.getDescr() );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	public void testParameterList(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		scope.inTransaction( entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Item> q = entityManager.createQuery( "select item from Item item where item.name in :names", Item.class );
			//test hint in value and string
			q.setHint( "org.hibernate.fetchSize", 10 );
			q.setHint( "org.hibernate.fetchSize", "10" );
			List<String> params = new ArrayList<>();
			params.add( item.getName() );
			q.setParameter( "names", params );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );

			q = entityManager.createQuery( "select item from Item item where item.name in :names", Item.class );
			//test hint in value and string
			q.setHint( "org.hibernate.fetchSize", 10 );
			q.setHint( "org.hibernate.fetchSize", "10" );
			params.add( item2.getName() );
			q.setParameter( "names", params );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );

			q = entityManager.createQuery( "select item from Item item where item.name in ?1", Item.class );
			params = new ArrayList<>();
			params.add( item.getName() );
			params.add( item2.getName() );
			// deprecated usage of positional parameter by String
			q.setParameter( 1, params );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );
		} );
	}

	@Test
	public void testParameterListInExistingParens(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		scope.inTransaction( entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Item> q = entityManager.createQuery( "select item from Item item where item.name in (:names)", Item.class );
			//test hint in value and string
			q.setHint( "org.hibernate.fetchSize", 10 );
			q.setHint( "org.hibernate.fetchSize", "10" );
			List<String> params = new ArrayList<>();
			params.add( item.getName() );
			params.add( item2.getName() );
			q.setParameter( "names", params );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );

			q = entityManager.createQuery( "select item from Item item where item.name in ( \n :names \n)\n", Item.class );
			//test hint in value and string
			q.setHint( "org.hibernate.fetchSize", 10 );
			q.setHint( "org.hibernate.fetchSize", "10" );
			params = new ArrayList<>();
			params.add( item.getName() );
			params.add( item2.getName() );
			q.setParameter( "names", params );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );

			q = entityManager.createQuery( "select item from Item item where item.name in ( ?1 )", Item.class );
			params = new ArrayList<>();
			params.add( item.getName() );
			params.add( item2.getName() );
			// deprecated usage of positional parameter by String
			q.setParameter( 1, params );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17490" )
	public void testEmptyParameterList(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		scope.inTransaction( entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Item> q = entityManager.createQuery(
					"select item from Item item where item.name in :names",
					Item.class
			);
			q.setParameter( "names", List.of() );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 0, result.size() );

			q = entityManager.createQuery(
					"select item from Item item where item.name not in :names",
					Item.class
			);
			q.setParameter( "names", List.of() );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArbitraryEscapeCharInLike.class)
	public void testEscapeCharacter(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Micro_oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		scope.inTransaction( entityManager -> {

			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Item> q = entityManager.createQuery( "select item from Item item where item.descr like 'Microk_oft mouse' escape 'k' ", Item.class );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
			int deleted = entityManager.createQuery( "delete from Item" ).executeUpdate();
			assertEquals( 2, deleted );
		} );
	}

	@Test
	public void testNativeQueryByEntity(EntityManagerFactoryScope scope) {
		Statistics stats = scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getStatistics();

		scope.inEntityManager( entityManager -> {

			entityManager.getTransaction().begin();
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
			entityManager.getTransaction().commit();

			stats.clear();
			assertEquals( 0, stats.getFlushCount() );

			entityManager.getTransaction().begin();
			item = (Item) entityManager.createNativeQuery( "select * from Item", Item.class ).getSingleResult();
			assertEquals( 1, stats.getFlushCount() );
			assertNotNull( item );
			assertEquals( "Micro$oft mouse", item.getDescr() );
			entityManager.getTransaction().commit();
		} );
	}

	@Test
	public void testNativeQueryByResultSet(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction(  entityManager -> {
			Item item = (Item) entityManager.createNativeQuery(
					"select name as itemname, descr as itemdescription from Item",
					"getItem"
			).getSingleResult();
			assertNotNull( item );
			assertEquals( "Micro$oft mouse", item.getDescr() );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTimestampComparison.class)
	public void testExplicitPositionalParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
		} );

		scope.inTransaction(  entityManager -> {
			TypedQuery<Wallet> query = entityManager.createQuery( "select w from " + Wallet.class.getName() + " w where w.brand in ?1", Wallet.class );
			List<String> brands = new ArrayList<>();
			brands.add( "Lacoste" );
			query.setParameter( 1, brands );
			Wallet w = query.getSingleResult();
			assertNotNull( w );
			query = entityManager.createQuery( "select w from " + Wallet.class.getName() + " w where w.marketEntrance = ?1", Wallet.class );
			query.setParameter( 1, new Date(), TemporalType.DATE );
			assertEquals( 0, query.getResultList().size() );
		} );
	}

	@Test
	public void testTemporalTypeBinding(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			TypedQuery<Wallet> query = entityManager.createQuery( "select w from " + Wallet.class.getName() + " w where w.marketEntrance = :me", Wallet.class );
			Parameter<Date> parameter = query.getParameter( "me", Date.class );
			assertEquals( java.util.Date.class, parameter.getParameterType() );

			query.setParameter( "me", new Date() );
			query.setParameter( "me", new Date(), TemporalType.DATE );
			query.setParameter( "me", new GregorianCalendar(), TemporalType.DATE );
		} );
	}

	@Test
	public void testPositionalParameterForms(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
		} );

		scope.inTransaction(  entityManager -> {
			// first using jpa-style positional parameter
			TypedQuery<Wallet> query = entityManager.createQuery( "select w from Wallet w where w.brand = ?1", Wallet.class );
			query.setParameter( 1, "Lacoste" );
			Wallet w = query.getSingleResult();
			assertNotNull( w );

			// next using jpa-style positional parameter, but as a name (which is how Hibernate core treats these
			query = entityManager.createQuery( "select w from Wallet w where w.brand = ?1", Wallet.class );
			// deprecated usage of positional parameter by String
			query.setParameter( 1, "Lacoste" );
			w = query.getSingleResult();
			assertNotNull( w );
		} );
	}

	@Test
	public void testPositionalParameterWithUserError(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
			entityManager.flush();

			// Gaps are not allowed
			IllegalArgumentException iae = assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.createQuery( "select w from Wallet w where w.brand = ?1 and w.model = ?3", Wallet.class ),
					"expecting error regarding gap in positional param labels"
			);
			assertNotNull( iae.getCause() );
			assertTyping( QueryException.class, iae.getCause() );
			assertTrue( iae.getCause().getMessage().contains( "Gap" ) );

			// using jpa-style, position index should match syntax '?<position>'.
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						Query jpaQuery = entityManager.createQuery( "select w from Wallet w where w.brand = ?1", Wallet.class );
						jpaQuery.setParameter( 1, "Lacoste" );
						jpaQuery.setParameter( 2, "Expensive" );
					},
					"Should fail due to a user error in parameters"
			);

			// using jpa-style, position index specified not in query - test exception type
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						Query jpaQuery = entityManager.createQuery( "select w from Wallet w " );
						jpaQuery.getParameter( 1 );
					},
					"Should fail due to a user error in parameters"
			);

			// using jpa-style, position index specified not in query - test exception type
			assertThrows(
					IllegalArgumentException.class,
					() -> {
						Query jpaQuery = entityManager.createQuery( "select w from Wallet w" );
						jpaQuery.getParameter( 1, Integer.class );
					},
					"Should fail due to a user error in parameters"
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-10803")
	public void testNamedParameterWithUserError(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
			entityManager.flush();

			assertThrows(
					IllegalArgumentException.class,
					() -> {
						Query jpaQuery = entityManager.createQuery( "select w from Wallet w", Wallet.class );
						jpaQuery.getParameter( "brand" );
					},
					"Should fail due to a user error in parameters"
			);

			assertThrows(
					IllegalArgumentException.class,
					() -> {
						Query jpaQuery = entityManager.createQuery( "select w from Wallet w", Wallet.class );
						jpaQuery.getParameter( "brand", String.class );
					},
					"Should fail due to a user error in parameters"
			);
		} );
	}

	@Test
	public void testNativeQuestionMarkParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
		} );

		scope.inTransaction(  entityManager -> {
			Query query = entityManager.createNativeQuery( "select * from Wallet w where w.brand = ?", Wallet.class );
			query.setParameter( 1, "Lacoste" );
			Wallet w = (Wallet) query.getSingleResult();
			assertNotNull( w );
		} );
	}

	@Test
	public void testNativeQueryWithPositionalParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction(  entityManager -> {
			Query query = entityManager.createNativeQuery( "select * from Item where name = ?1", Item.class );
			query.setParameter( 1, "Mouse" );
			Item item = (Item) query.getSingleResult();
			assertNotNull( item );
			assertEquals( "Micro$oft mouse", item.getDescr() );
			query = entityManager.createNativeQuery( "select * from Item where name = ?", Item.class );
			query.setParameter( 1, "Mouse" );
			item = (Item) query.getSingleResult();
			assertNotNull( item );
			assertEquals( "Micro$oft mouse", item.getDescr() );
		} );
	}

	@Test
	public void testDistinct(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Distributor d1 = new Distributor();
			d1.setName( "Fnac" );
			Distributor d2 = new Distributor();
			d2.setName( "Darty" );
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			item.getDistributors().add( d1 );
			item.getDistributors().add( d2 );
			entityManager.persist( d1 );
			entityManager.persist( d2 );
			entityManager.persist( item );
			entityManager.flush();
			entityManager.clear();

			TypedQuery<Item> q = entityManager.createQuery( "select distinct i from Item i left join fetch i.distributors", Item.class );
			item = q.getSingleResult();
			assertTrue( Hibernate.isInitialized( item.getDistributors() ) );
			assertEquals( 2, item.getDistributors().size() );
		} );
	}

	@Test
	public void testIsNull(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Distributor d1 = new Distributor();
			d1.setName( "Fnac" );
			Distributor d2 = new Distributor();
			d2.setName( "Darty" );
			Item item = new Item( "Mouse", null );
			Item item2 = new Item( "Mouse2", "dd" );
			item.getDistributors().add( d1 );
			item.getDistributors().add( d2 );
			entityManager.persist( d1 );
			entityManager.persist( d2 );
			entityManager.persist( item );
			entityManager.persist( item2 );
			entityManager.flush();
			entityManager.clear();

			TypedQuery<Item> q = entityManager.createQuery(
					"select i from Item i where i.descr = :descr or (i.descr is null and cast(:descr as string) is null)", Item.class
			);
			q.setParameter( "descr", "dd" );
			List<Item> result = q.getResultList();
			assertEquals( 1, result.size() );
			q.setParameter( "descr", null );
			result = q.getResultList();
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	public void testUpdateQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
			entityManager.flush();
			entityManager.clear();

			assertEquals(
					1, entityManager.createNativeQuery(
							"update Item set descr = 'Logitech Mouse' where name = 'Mouse'", Item.class
					).executeUpdate()
			);
			item = entityManager.find( Item.class, item.getName() );
			assertEquals( "Logitech Mouse", item.getDescr() );
		} );
	}

	@Test
	public void testUnavailableNamedQuery(EntityManagerFactoryScope scope) {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		scope.inTransaction( entityManager -> {
			entityManager.persist( item );

			assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.createNamedQuery( "wrong name" ),
					"Wrong named query should raise an exception"
			);

			assertTrue(
					entityManager.getTransaction().getRollbackOnly(),
					"thrown IllegalArgumentException should have caused transaction to be marked for rollback only"
			);
		} );

		scope.inTransaction( entityManager -> {
			assertNull( entityManager.find( Item.class, item.getName() ),
					"entity should not have been saved to database since IllegalArgumentException should have " +
					"caused transaction to be marked for rollback only"
			);
		} );
	}

	@Test
	public void testTypedNamedNativeQuery(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction(  entityManager -> {
			Item item = entityManager.createNamedQuery( "nativeItem1", Item.class ).getSingleResult();
			item = entityManager.createNamedQuery( "nativeItem2", Item.class ).getSingleResult();
			assertNotNull( item );
			assertEquals( "Micro$oft mouse", item.getDescr() );
		} );
	}

	@Test
	public void testTypedScalarQueries(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
		} );

		scope.inTransaction(  entityManager -> {
			Object[] itemData = entityManager.createQuery( "select i.name,i.descr from Item i", Object[].class ).getSingleResult();
			assertEquals( 2, itemData.length );
			assertEquals( String.class, itemData[0].getClass() );
			assertEquals( String.class, itemData[1].getClass() );
			Tuple itemTuple = entityManager.createQuery( "select i.name,i.descr from Item i", Tuple.class ).getSingleResult();
			assertEquals( 2, itemTuple.getElements().size() );
			assertEquals( String.class, itemTuple.get( 0 ).getClass() );
			assertEquals( String.class, itemTuple.get( 1 ).getClass() );
			Item itemView = entityManager.createQuery( "select new Item(i.name,i.descr) from Item i", Item.class )
					.getSingleResult();
			assertNotNull( itemView );
			assertEquals( "Micro$oft mouse", itemView.getDescr() );
			itemView = entityManager.createNamedQuery( "query-construct", Item.class ).getSingleResult();
			assertNotNull( itemView );
			assertEquals( "Micro$oft mouse", itemView.getDescr() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10269")
	public void testFailingNativeQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					PersistenceException.class,
					() -> {
						// Tests that Oracle does not run out of cursors.
						for ( int i = 0; i < 1000; i++ ) {
							entityManager.createNativeQuery( "Select 1 from NonExistentTable" ).getResultList();
						}
					},
					"Expected PersistenceException"
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-10833")
	public void testGetSingleResultWithNoResultException(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			assertThrows(
					NoResultException.class,
					() -> entityManager.createQuery( "FROM Item WHERE name = 'bozo'" ).getSingleResult(),
					"Expected NoResultException"
			);
		} );
	}

	@Test
	public void testGetSingleResultWithManyResultsException(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Item( "1", "1" ) );
			entityManager.persist( new Item( "2", "2" ) );
			assertThrows(
					NonUniqueResultException.class,
					() -> entityManager.createQuery( "FROM Item" ).getSingleResult(),
					"Expected NonUniqueResultException"
			);
		} );
	}
}
