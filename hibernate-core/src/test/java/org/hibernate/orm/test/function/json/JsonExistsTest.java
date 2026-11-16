package org.hibernate.orm.test.function.json;

import java.util.HashMap;
import java.util.List;

import jakarta.persistence.Tuple;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.sql.exec.ExecutionException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.function.json.JsonTestHelper.assertNoInjection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithJson.class)
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonExists.class)
public class JsonExistsTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			EntityWithJson entity = new EntityWithJson();
			entity.setId( 1L );
			entity.getJson().put( "theInt", 1 );
			entity.getJson().put( "theFloat", 0.1 );
			entity.getJson().put( "theString", "abc" );
			entity.getJson().put( "theBoolean", true );
			entity.getJson().put( "theNull", null );
			entity.getJson().put( "theArray", new String[] { "a", "b", "c" } );
			entity.getJson().put( "theObject", new HashMap<>( entity.getJson() ) );
			em.persist(entity);
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-exists-example[]
			List<Boolean> results = em.createQuery( "select json_exists(e.json, '$.theString') from EntityWithJson e", Boolean.class )
					.getResultList();
			//end::hql-json-exists-example[]
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, versionMatchMode = VersionMatchMode.OLDER, reason = "Oracle bug in versions before 23")
	public void testPassing(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-exists-passing-example[]
			List<Boolean> results = em.createQuery( "select json_exists(e.json, '$.theArray[$idx]' passing 1 as idx) from EntityWithJson e", Boolean.class )
					.getResultList();
			//end::hql-json-exists-passing-example[]
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB reports the error 4038 as warning and simply returns null")
	public void testOnError(SessionFactoryScope scope) {
		scope.inSession( em -> {
			try {
				//tag::hql-json-exists-on-error-example[]
				em.createQuery( "select json_exists('invalidJson', '$.theInt' error on error) from EntityWithJson e")
						.getResultList();
				//end::hql-json-exists-on-error-example[]
				fail("error clause should fail because of invalid json document");
			}
			catch ( HibernateException e ) {
				if ( !( e instanceof JDBCException ) && !( e instanceof ExecutionException ) ) {
					throw e;
				}
			}
		} );
	}

	@Test
	public void testPathInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_exists(e.json, :path) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPassingInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_exists(e.json, '$[$a]' passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "val", "'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPathPassingInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_exists(e.json, :path passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$'--" )
					.setParameter( "val", "'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPathBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_exists(e.json, :path) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$\\'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPassingBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_exists(e.json, '$[$a]' passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "val", "\\'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPathPassingBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_exists(e.json, :path passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$\\'--" )
					.setParameter( "val", "\\'--" )
					.getResultList();
		} );
	}

	@Test
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "The H2 emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "The SQL Server emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "The CockroachDB emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = SingleStoreDialect.class, reason = "The SingleStore emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 doesn't support quoted member names in JSON paths")
	public void testPathQuoteRoundTrip(SessionFactoryScope scope) {
		JsonValueTest.persistSpecialCharacterEntity( scope );
		scope.inSession( em -> {
			assertEquals( true, em.createQuery( "select json_exists(e.json, :path) from EntityWithJson e where e.id = 2", Boolean.class )
					.setParameter( "path", "$.\"a'b\"" )
					.getSingleResult() );
			assertEquals( false, em.createQuery( "select json_exists(e.json, :path) from EntityWithJson e where e.id = 2", Boolean.class )
					.setParameter( "path", "$.\"a''b\"" )
					.getSingleResult() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "The H2 emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "The SQL Server emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "The CockroachDB emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = SingleStoreDialect.class, reason = "The SingleStore emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 doesn't support quoted member names in JSON paths")
	public void testPathBackslashRoundTrip(SessionFactoryScope scope) {
		JsonValueTest.persistSpecialCharacterEntity( scope );
		scope.inSession( em -> {
			assertEquals( true, em.createQuery( "select json_exists(e.json, :path) from EntityWithJson e where e.id = 2", Boolean.class )
					.setParameter( "path", "$.\"a\\\\b\"" )
					.getSingleResult() );
		} );
	}

}
