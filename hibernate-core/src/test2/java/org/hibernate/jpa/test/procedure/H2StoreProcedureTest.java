/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.procedure;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
public class H2StoreProcedureTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {MyEntity.class};
	}

	@Before
	public void setUp() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.createNativeQuery( "CREATE ALIAS get_all_entities FOR \"" + H2StoreProcedureTest.class.getCanonicalName() + ".getAllEntities\";" )
					.executeUpdate();

			entityManager.createNativeQuery( "CREATE ALIAS by_id FOR \"" + H2StoreProcedureTest.class.getCanonicalName() + ".entityById\";" )
					.executeUpdate();
			MyEntity entity = new MyEntity();
			entity.id = 1;
			entity.name = "entity1";
			entityManager.persist( entity );

			entityManager.getTransaction().commit();

		}
		catch (Exception e) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@After
	public void tearDown() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.createNativeQuery( "DROP ALIAS IF EXISTS get_all_entities" ).executeUpdate();
			entityManager.createNativeQuery( "DROP ALIAS IF EXISTS by_id" ).executeUpdate();
			entityManager.getTransaction().commit();
		}
		catch (Exception e) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	public static ResultSet getAllEntities(Connection conn) throws SQLException {
		return conn.createStatement().executeQuery( "select * from MY_ENTITY" );
	}

	public static ResultSet entityById(Connection conn, long id) throws SQLException {
		return conn.createStatement().executeQuery( "select * from MY_ENTITY where id = " + Long.toString( id ) );
	}

	@Test
	public void testStoreProcedureGetParameters() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "get_all_entities", MyEntity.class );
			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 0 ) );

			final List resultList = query.getResultList();
			assertThat( resultList.size(), is( 1 ) );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testStoreProcedureGetParameterByPosition() {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "by_Id", MyEntity.class );
			query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );

			query.setParameter( 1, 1L );

			final List resultList = query.getResultList();
			assertThat( resultList.size(), is( 1 ) );

			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 1 ) );

			final Parameter<?> parameter = query.getParameter( 1 );
			assertThat( parameter, not( nullValue() ) );

			try {
				query.getParameter( 2 );
				fail( "IllegalArgumentException expected, parameter at position 2 does not exist" );
			}
			catch (IllegalArgumentException iae) {
				//expected
			}
		}
		finally {
			entityManager.close();
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		long id;

		String name;
	}
}
