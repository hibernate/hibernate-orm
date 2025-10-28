/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.orm.test.jpa.Book;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.EmbeddedDataColumn;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.orm.test.jpa.SubBook;
import org.hibernate.orm.test.jpa.ValueEmbeddedDataColumn;
import org.hibernate.orm.test.jpa.Wallet;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import junit.framework.Assert;

import static junit.framework.Assert.assertNull;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 * @author Chris Cranford
 * @author Yanming Zhou
 */
public class QueryTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Distributor.class,
				Wallet.class,
				Employee.class,
				Contractor.class,
				Book.class,
				SubBook.class,
				EmbeddedDataColumn.class,
				ValueEmbeddedDataColumn.class,
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7192")
	public void testTypedManipulationQueryError() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			em.createQuery( "delete Item", Item.class );
			fail();
		}
		catch (IllegalArgumentException expected) {
			//expected
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}

		try {
			em.createQuery( "update Item i set i.name = 'someName'", Item.class );
			fail();
		}
		catch (IllegalArgumentException expected) {
			//expected
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testPagedQuery() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testNullPositionalParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			Query q = em.createQuery( "from Item i where i.intVal=?1" );
			q.setParameter( 1, null );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null and ?1 is null" );
			q.setParameter( 1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null or i.intVal = ?1" );
			q.setParameter( 1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testNullPositionalParameterParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			Query q = em.createQuery( "from Item i where i.intVal=?1" );
			Parameter p = new Parameter() {
				@Override
				public String getName() {
					return null;
				}

				@Override
				public Integer getPosition() {
					return 1;
				}

				@Override
				public Class getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			Parameter pGotten = q.getParameter( p.getPosition() );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null and ?1 is null" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null or i.intVal = ?1" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testNullPositionalParameterParameterIncompatible() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			Query q = em.createQuery( "from Item i where i.intVal=?1" );
			Parameter p = new Parameter() {
				@Override
				public String getName() {
					return null;
				}

				@Override
				public Integer getPosition() {
					return 1;
				}

				@Override
				public Class getParameterType() {
					return Long.class;
				}
			};
			q.setParameter( p, null );
			Parameter pGotten = q.getParameter( p.getPosition() );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null and ?1 is null" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null or i.intVal = ?1" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testNullNamedParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			Query q = em.createQuery( "from Item i where i.intVal=:iVal" );
			q.setParameter( "iVal", null );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null and :iVal is null" );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null or i.intVal = :iVal" );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testNullNamedParameterParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			Query q = em.createQuery( "from Item i where i.intVal=:iVal" );
			Parameter p = new Parameter() {
				@Override
				public String getName() {
					return "iVal";
				}

				@Override
				public Integer getPosition() {
					return null;
				}

				@Override
				public Class getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null and :iVal is null" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null or i.intVal = :iVal" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testNullNamedParameterParameterIncompatible() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			Query q = em.createQuery( "from Item i where i.intVal=:iVal" );
			Parameter p = new Parameter() {
				@Override
				public String getName() {
					return "iVal";
				}

				@Override
				public Integer getPosition() {
					return null;
				}

				@Override
				public Class getParameterType() {
					return Long.class;
				}
			};
			q.setParameter( p, null );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null and :iVal is null" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createQuery( "from Item i where i.intVal is null or i.intVal = :iVal" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@SkipForDialect(value = PostgreSQLDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = PostgresPlusDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = CockroachDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullPositionalParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			// native queries don't seem to flush by default ?!?
			em.flush();
			Query q = em.createNativeQuery( "select * from Item i where i.int_val=?" );
			q.setParameter( 1, null );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createNativeQuery( "select * from Item i where i.int_val is null and ? is null" );
			q.setParameter( 1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = ?" );
			q.setParameter(1, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10161")
	@SkipForDialect(value = PostgreSQLDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = PostgresPlusDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = CockroachDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullPositionalParameterParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			// native queries don't seem to flush by default ?!?
			em.flush();
			Query q = em.createNativeQuery( "select * from Item i where i.int_val=?" );
			Parameter p = new Parameter() {
				@Override
				public String getName() {
					return null;
				}

				@Override
				public Integer getPosition() {
					return 1;
				}

				@Override
				public Class getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			Parameter pGotten = q.getParameter( p.getPosition() );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createNativeQuery( "select * from Item i where i.int_val is null and ? is null" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = ?" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@SkipForDialect(value = PostgreSQLDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = PostgresPlusDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = CockroachDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullNamedParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			// native queries don't seem to flush by default ?!?
			em.flush();
			Query q = em.createNativeQuery( "select * from Item i where i.int_val=:iVal" );
			q.setParameter( "iVal", null );
			List results = q.getResultList();
			// null != null
			assertEquals( 0, results.size() );
			q = em.createNativeQuery( "select * from Item i where (i.int_val is null) and (:iVal is null)" );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = :iVal" );
			q.setParameter( "iVal", null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10161")
	@SkipForDialect(value = PostgreSQLDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = PostgresPlusDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	@SkipForDialect(value = CockroachDialect.class, jiraKey = "HHH-10312", comment = "Cannot determine the parameter types and bind type is unknown because the value is null")
	public void testNativeQueryNullNamedParameterParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			// native queries don't seem to flush by default ?!?
			em.flush();
			Query q = em.createNativeQuery( "select * from Item i where i.int_val=:iVal" );
			Parameter p = new Parameter() {
				@Override
				public String getName() {
					return "iVal";
				}

				@Override
				public Integer getPosition() {
					return null;
				}

				@Override
				public Class getParameterType() {
					return Integer.class;
				}
			};
			q.setParameter( p, null );
			Parameter pGotten = q.getParameter( p.getName() );
			List results = q.getResultList();
			assertEquals( 0, results.size() );
			q = em.createNativeQuery( "select * from Item i where (i.int_val is null) and (:iVal is null)" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
			q = em.createNativeQuery( "select * from Item i where i.int_val is null or i.int_val = :iVal" );
			q.setParameter( p, null );
			results = q.getResultList();
			assertEquals( 1, results.size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@JiraKey("HHH-18033")
	public void testQueryContainsQuotedSemicolonWithLimit() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( new Item( "Mouse;", "Micro$oft mouse" ) );

			Query q = em.createQuery( "from Item where name like '%;%'" ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );

			q = em.createQuery( "from Item where name like '%;%' " ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@JiraKey("HHH-18033")
	public void testNativeQueryContainsQuotedSemicolonWithLimit() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( new Item( "Mouse;", "Micro$oft mouse" ) );

			Query q = em.createNativeQuery( "select * from Item where name like '%;%'" ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );

			q = em.createNativeQuery( "select * from Item where name like '%;%' " ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@JiraKey("HHH-18033")
	@SkipForDialect(value = OracleDialect.class, comment = "Doesn't support semicolon as ending of statement")
	@SkipForDialect(value = SybaseDialect.class, comment = "Doesn't support semicolon as ending of statement")
	@SkipForDialect(value = DerbyDialect.class, comment = "Doesn't support semicolon as ending of statement")
	@SkipForDialect(value = DB2Dialect.class, comment = "Doesn't support semicolon as ending of statement")
	public void testNativeQueryContainsQuotedSemicolonAndEndsWithSemicolonWithLimit() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( new Item( "Mouse;", "Micro$oft mouse" ) );

			Query q = em.createNativeQuery( "select * from Item where name like '%;%';" ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );

			q = em.createNativeQuery( "select * from Item where name like '%;%' ; " ).setMaxResults(10);
			assertEquals( 1, q.getResultList().size() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testAggregationReturnType() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Item item = new Item( "Mouse", "Micro$oft mouse" );
			em.persist( item );
			item = new Item( "Computer", "Apple II" );
			em.persist( item );
			Query q = em.createQuery( "select count(i) from Item i where i.name like :itemName" );
			q.setParameter( "itemName", "%" );
			assertTrue( q.getSingleResult() instanceof Long );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testTypeExpression() throws Exception {
		final EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			final Employee employee = new Employee( "Lukasz", 100.0 );
			em.persist( employee );
			final Contractor contractor = new Contractor( "Kinga", 100.0, "Microsoft" );
			em.persist( contractor );
			final Query q = em.createQuery( "SELECT e FROM Employee e where TYPE(e) <> Contractor" );
			final List result = q.getResultList();
			assertNotNull( result );
			assertEquals( Arrays.asList( employee ), result );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH_7407")
	public void testMultipleParameterLists() throws Exception {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
			q = em.createQuery(
					"select item from Item item where item.name in :names and ( item.descr is null or item.descr in :descrs )" );
			q.setParameter( "names", names );
			q.setParameter( "descrs", descrs );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );

			em.getTransaction().begin();
			em.remove( em.getReference( Item.class, item.getName() ) );
			em.remove( em.getReference( Item.class, item2.getName() ) );
			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH_8949")
	public void testCacheStoreAndRetrieveModeParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Query query = em.createQuery( "select item from Item item" );

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

			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testJpaPositionalParameters() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Query query = em.createQuery( "from Item item where item.name =?1 or item.descr = ?1" );
			Parameter p1 = query.getParameter( 1 );
			Assert.assertNotNull( p1 );
			Assert.assertNotNull( p1.getPosition() );
			Assert.assertNull( p1.getName() );

			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParameterCollectionAndPositional() {
		final Item item = new Item( "Mouse", "Microsoft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Query q = entityManager.createQuery( "select item from Item item where item.name in ?1 and item.descr = ?2" );
			List params = new ArrayList();
			params.add( item.getName() );
			params.add( item2.getName() );
			q.setParameter( 1, params );
			q.setParameter( 2, item2.getDescr() );
			List result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
		} );

	}

	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParameterCollectionParenthesesAndPositional() {
		final Item item = new Item( "Mouse", "Microsoft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Query q = entityManager.createQuery(
					"select item from Item item where item.name in (?1) and item.descr = ?2" );
			List params = new ArrayList();
			params.add( item.getName() );
			params.add( item2.getName() );
			q.setParameter( 1, params );
			q.setParameter( 2, item2.getDescr() );
			List result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12290")
	public void testParameterCollectionSingletonParenthesesAndPositional() {
		final Item item = new Item( "Mouse", "Microsoft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( item );
			entityManager.persist( item2 );
			assertTrue( entityManager.contains( item ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Query q = entityManager.createQuery(
					"select item from Item item where item.name in (?1) and item.descr = ?2" );
			List params = new ArrayList();
			params.add( item2.getName() );
			q.setParameter( 1, params );
			q.setParameter( 2, item2.getDescr() );
			List result = q.getResultList();
			assertNotNull( result );
			assertEquals( 1, result.size() );
		} );
	}

	@Test
	public void testParameterList() throws Exception {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
			q.setParameter( 1, params );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );
			em.remove( result.get( 0 ) );
			em.remove( result.get( 1 ) );
			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testParameterListInExistingParens() throws Exception {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
			q.setParameter( 1, params );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );
			em.remove( result.get( 0 ) );
			em.remove( result.get( 1 ) );
			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17490" )
	public void testEmptyParameterList() throws Exception {
		final Item item = new Item( "Mouse", "Micro$oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( item );
			em.persist( item2 );
			assertTrue( em.contains( item ) );
			em.getTransaction().commit();

			em.getTransaction().begin();
			TypedQuery<Item> q = em.createQuery(
					"select item from Item item where item.name in :names",
					Item.class
			);
			q.setParameter( "names", List.of() );
			List<Item> result = q.getResultList();
			assertNotNull( result );
			assertEquals( 0, result.size() );

			q = em.createQuery(
					"select item from Item item where item.name not in :names",
					Item.class
			);
			q.setParameter( "names", List.of() );
			result = q.getResultList();
			assertNotNull( result );
			assertEquals( 2, result.size() );

			em.remove( result.get( 0 ) );
			em.remove( result.get( 1 ) );
			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testEscapeCharacter() throws Exception {
		final Item item = new Item( "Mouse", "Micro_oft mouse" );
		final Item item2 = new Item( "Computer", "Dell computer" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testNativeQueryByEntity() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testNativeQueryByResultSet() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( item );
			assertTrue( em.contains( item ) );
			em.getTransaction().commit();

			em.getTransaction().begin();
			item = (Item) em.createNativeQuery(
					"select name as itemname, descr as itemdescription from Item",
					"getItem"
			)
					.getSingleResult();
			assertNotNull( item );
			assertEquals( "Micro$oft mouse", item.getDescr() );
			em.remove( item );
			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testExplicitPositionalParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testTemporalTypeBinding() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Query query = em.createQuery( "select w from " + Wallet.class.getName() + " w where w.marketEntrance = :me" );
			Parameter parameter = query.getParameter( "me", Date.class );
			assertEquals( parameter.getParameterType(), java.sql.Timestamp.class );

			query.setParameter( "me", new Date() );
			query.setParameter( "me", new Date(), TemporalType.DATE );
			query.setParameter( "me", new GregorianCalendar(), TemporalType.DATE );

			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}

	}

	@Test
	public void testPositionalParameterForms() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
			query.setParameter( 1, "Lacoste" );
			w = (Wallet) query.getSingleResult();
			assertNotNull( w );

			em.remove( w );
			em.getTransaction().commit();
		}
		catch (Exception e){
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testPositionalParameterWithUserError() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			em.persist( w );
			em.flush();

			// Gaps are not allowed
			try {
				Query jpaQuery = em.createQuery( "select w from Wallet w where w.brand = ?1 and w.model = ?3" );
				fail( "expecting error regarding gap in positional param labels" );
			}
			catch ( IllegalArgumentException e ) {
				assertNotNull( e.getCause() );
				assertTyping( QueryException.class, e.getCause() );
				assertTrue( e.getCause().getMessage().contains( "Gap" ) );
			}

			// using jpa-style, position index should match syntax '?<position>'.
			Query jpaQuery = em.createQuery( "select w from Wallet w where w.brand = ?1" );
			jpaQuery.setParameter( 1, "Lacoste" );
			try {
				jpaQuery.setParameter( 2, "Expensive" );
				fail( "Should fail due to a user error in parameters" );
			}
			catch (Exception e) {
				assertTyping( IllegalArgumentException.class, e );
			}

			// using jpa-style, position index specified not in query - test exception type
			jpaQuery = em.createQuery( "select w from Wallet w " );
			try {
				Parameter parameter = jpaQuery.getParameter( 1 );
				fail( "Should fail due to a user error in parameters" );
			}
			catch (Exception e) {
				assertTyping( IllegalArgumentException.class, e );
			}

			// using jpa-style, position index specified not in query - test exception type
			jpaQuery = em.createQuery( "select w from Wallet w" );
			try {
				Parameter<Integer> parameter = jpaQuery.getParameter( 1, Integer.class );
				fail( "Should fail due to user error in parameters" );
			}
			catch (Exception e) {
				assertTyping( IllegalArgumentException.class, e );
			}
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10803")
	public void testNamedParameterWithUserError() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			em.persist( w );
			em.flush();

			Query jpaQuery = em.createQuery( "select w from Wallet w" );
			try {
				Parameter<?> parameter = jpaQuery.getParameter( "brand" );
				fail( "Should fail due to user error in parameters" );
			}
			catch (Exception e) {
				assertTyping( IllegalArgumentException.class, e );
			}

			jpaQuery = em.createQuery( "select w from Wallet w" );
			try {
				Parameter<String> parameter = jpaQuery.getParameter( "brand", String.class );
				fail( "Should fail due to user error in parameters" );
			}
			catch (Exception e) {
				assertTyping( IllegalArgumentException.class, e );
			}

		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testNativeQuestionMarkParameter() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		catch (Exception e) {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {

			em.close();
		}
	}

	@Test
	public void testNativeQueryWithPositionalParameter() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		catch (Exception e) {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testDistinct() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testIsNull() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testUpdateQuery() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testUnavailableNamedQuery() throws Exception {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( item );
			try {
				em.createNamedQuery( "wrong name" );
				fail( "Wrong named query should raise an exception" );
			}
			catch (IllegalArgumentException e) {
				//success
			}
			assertTrue(
					"thrown IllegalArgumentException should of caused transaction to be marked for rollback only",
					true == em.getTransaction().getRollbackOnly()
			);
			em.getTransaction().rollback();        // HHH-8442 changed to rollback since thrown ISE causes
			// transaction to be marked for rollback only.
			// No need to remove entity since it was rolled back.

			assertNull(
					"entity should not of been saved to database since IllegalArgumentException should of" +
							"caused transaction to be marked for rollback only", em.find( Item.class, item.getName() )
			);
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			em.close();
		}
	}

	@Test
	public void testTypedNamedNativeQuery() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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

		}
		catch (Exception e) {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testTypedScalarQueries() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
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
			Item itemView = em.createQuery( "select new Item(i.name,i.descr) from Item i", Item.class )
					.getSingleResult();
			assertNotNull( itemView );
			assertEquals( "Micro$oft mouse", itemView.getDescr() );
			itemView = em.createNamedQuery( "query-construct", Item.class ).getSingleResult();
			assertNotNull( itemView );
			assertEquals( "Micro$oft mouse", itemView.getDescr() );
			em.remove( item );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10269")
	public void testFailingNativeQuery() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			// Tests that Oracle does not run out of cursors.
			for ( int i = 0; i < 1000; i++ ) {
				try {
					entityManager.createNativeQuery( "Select 1 from NotExistedTable" ).getResultList();
					fail( "expected PersistenceException" );
				}
				catch (PersistenceException e) {
					// expected
				}
			}
		}finally {
			entityManager.close();
		}

	}

	@Test
	@TestForIssue(jiraKey = "HHH-10833")
	public void testGetSingleResultWithNoResultException() {
		final EntityManager entityManager  = getOrCreateEntityManager();
		try {
			entityManager.createQuery( "FROM Item WHERE name = 'bozo'" ).getSingleResult();
			fail( "Expected NoResultException" );
		}
		catch ( Exception e ) {
			assertTyping( NoResultException.class, e );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testGetSingleResultWithManyResultsException() {
		final EntityManager entityManager  = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.persist( new Item( "1", "1" ) );
			entityManager.persist( new Item( "2", "2" ) );
			entityManager.createQuery( "FROM Item" ).getSingleResult();
			fail( "Expected NoResultException" );
		}
		catch ( Exception e ) {
			assertTyping( NonUniqueResultException.class, e );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-18236")
	public void test18236() {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();
			em.persist( new Book("9781932394153", "Hibernate in Action") );
			em.flush();

			List results = em.createNativeQuery("select b.* from book b", Book.class).getResultList();
			assertEquals(1, results.size());
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}

	}

}
