/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.function.json;

import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.dialect.MariaDBDialect;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithJson.class)
@SessionFactory
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
		scope.inTransaction( em -> {
			em.createMutationQuery( "delete from EntityWithJson" ).executeUpdate();
		} );
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

}
