/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.json;

import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.sql.exec.ExecutionException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithJson.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
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
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB reports the error 4038 as warning and simply returns null")
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
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsJsonValueErrorBehavior.class)
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

}
