package org.hibernate.orm.test.function.json;

import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.community.dialect.SingleStoreDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.sql.exec.ExecutionException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.hibernate.orm.test.function.json.JsonTestHelper.assertNoInjection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithJson.class)
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonValue.class)
public class JsonValueTest {

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
			//tag::hql-json-value-example[]
			List<Tuple> results = em.createQuery( "select json_value(e.json, '$.theString') from EntityWithJson e", Tuple.class )
					.getResultList();
			//end::hql-json-value-example[]
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testPassing(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-value-passing-example[]
			List<Tuple> results = em.createQuery( "select json_value(e.json, '$.theArray[$idx]' passing 1 as idx) from EntityWithJson e", Tuple.class )
					.getResultList();
			//end::hql-json-value-passing-example[]
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testReturning(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-value-returning-example[]
			List<Tuple> results = em.createQuery( "select json_value(e.json, '$.theInt' returning Integer) from EntityWithJson e", Tuple.class )
					.getResultList();
			//end::hql-json-value-returning-example[]
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonFunctionErrorBehavior.class)
	public void testOnError(SessionFactoryScope scope) {
		scope.inSession( em -> {
			try {
				//tag::hql-json-value-on-error-example[]
				em.createQuery( "select json_value('invalidJson', '$.theInt' error on error) from EntityWithJson e")
						.getResultList();
				//end::hql-json-value-on-error-example[]
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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonFunctionErrorBehavior.class)
	public void testOnEmpty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			try {
				//tag::hql-json-value-on-empty-example[]
				em.createQuery("select json_value(e.json, '$.nonExisting' error on empty error on error) from EntityWithJson e" )
						.getResultList();
				//end::hql-json-value-on-empty-example[]
				fail("empty clause should fail because of json path doesn't produce results");
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
			em.createQuery( "select json_value(e.json, :path) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPassingInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_value(e.json, '$[$a]' passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "val", "'--" )
					.getResultList();
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonFunctionErrorBehavior.class)
	public void testPassingInjectionError(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_value(e.json, '$[$a]' passing :val as a error on empty) from EntityWithJson e", Tuple.class )
					.setParameter( "val", "'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPathPassingInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_value(e.json, :path passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$'--" )
					.setParameter( "val", "'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPathBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_value(e.json, :path) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$\\'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPassingBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_value(e.json, '$[$a]' passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "val", "\\'--" )
					.getResultList();
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonFunctionErrorBehavior.class)
	public void testPassingBackslashInjectionError(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_value(e.json, '$[$a]' passing :val as a error on empty) from EntityWithJson e", Tuple.class )
					.setParameter( "val", "\\'--" )
					.getResultList();
		} );
	}

	@Test
	public void testPathPassingBackslashInjection(SessionFactoryScope scope) {
		assertNoInjection( scope, em -> {
			em.createQuery( "select json_value(e.json, :path passing :val as a) from EntityWithJson e", Tuple.class )
					.setParameter( "path", "$\\'--" )
					.setParameter( "val", "\\'--" )
					.getResultList();
		} );
	}

	@Test
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "The H2 emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "The CockroachDB emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = SingleStoreDialect.class, reason = "The SingleStore emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 doesn't support quoted member names in JSON paths")
	public void testPathQuoteRoundTrip(SessionFactoryScope scope) {
		persistSpecialCharacterEntity( scope );
		scope.inSession( em -> {
			final String result = em.createQuery( "select json_value(e.json, :path) from EntityWithJson e where e.id = 2", String.class )
					.setParameter( "path", "$.\"a'b\"" )
					.getSingleResult();
			assertEquals( "x'y", result );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "The H2 emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "The CockroachDB emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = SingleStoreDialect.class, reason = "The SingleStore emulation doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 doesn't support quoted member names in JSON paths")
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner's JSONPath doesn't support the \\\\ escape sequence in quoted member names")
	public void testPathBackslashRoundTrip(SessionFactoryScope scope) {
		persistSpecialCharacterEntity( scope );
		scope.inSession( em -> {
			// The JSON path escape sequence \\ denotes a single backslash in the member name.
			// If the backslashes were not properly escaped in the SQL literal on databases that
			// treat the backslash as escape character, the path would contain \b instead
			final String result = em.createQuery( "select json_value(e.json, :path) from EntityWithJson e where e.id = 2", String.class )
					.setParameter( "path", "$.\"a\\\\b\"" )
					.getSingleResult();
			assertEquals( "x\\y", result );
		} );
	}

	static void persistSpecialCharacterEntity(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			EntityWithJson entity = new EntityWithJson();
			entity.setId( 2L );
			entity.getJson().put( "a'b", "x'y" );
			entity.getJson().put( "a\\b", "x\\y" );
			em.persist( entity );
		} );
	}

}
