/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dialect.functional;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * Test pagination on newer SQL Server Dialects where the application explicitly specifies
 * the legacy {@code SQLServerDialect} instead and will fail on pagination queries.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11642")
@RequiresDialect(SQLServerDialect.class)
public class SQLServerDialectPaginationTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@Override
	protected Dialect getDialect() {
		// if the environment is any version of SQLServerDialect, force the legacy SQLServerDialect instead
		// This is so that the legacy's TopLimitHandler will be used here to test the fix necessary when a
		// user explicitly configures the legacy dialect but uses a more modern version of SQL Server.
		final Dialect environmentDialect = super.getDialect();
		if ( environmentDialect instanceof SQLServerDialect ) {
			return new SQLServerDialect();
		}
		return environmentDialect;
	}

	@Test
	public void testPaginationQuery() {
		// prepare some test data
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 1; i <= 20; ++i ) {
				final SimpleEntity entity = new SimpleEntity( i, "Entity" + i );
				entityManager.persist( entity );
			}
		} );

		// This would fail with "index 2 out of range" within TopLimitHandler
		// The fix addresses this problem which only occurs when using SQLServerDialect explicitly.
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<SimpleEntity> results = entityManager
					.createQuery( "SELECT o FROM SimpleEntity o WHERE o.id >= :firstId ORDER BY o.id", SimpleEntity.class )
					.setParameter( "firstId", 10 )
					.setMaxResults( 5 )
					.getResultList();
			// verify that the paginated query returned the right ids.
			final List<Integer> ids = results.stream().map( SimpleEntity::getId ).collect( Collectors.toList() );
			assertEquals( Arrays.asList( 10, 11, 12, 13, 14 ), ids );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-17700")
	public void testParameterizedPaginatedNativeQueries() {
		// prepare some test data
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 1; i <= 20; ++i ) {
				final SimpleEntity entity = new SimpleEntity( i, "Entity" + i );
				entityManager.persist( entity );
			}
		} );

		final String query1 = "\n"
				+ "DECLARE @myParam1 VARCHAR(15);\n"
				+ "DECLARE @myParam2 VARCHAR(15);\n"
				+ "SET @myParam1 = :firstId ;\n"
				+ "SELECT o.id FROM SimpleEntity o WHERE o.id >= @myParam1 ORDER BY o.id";

		final String query2 = "\n"
				+ "DECLARE @myParam1 VARCHAR(15);\n"
				+ "DECLARE @myParam2 VARCHAR(15);\n"
				+ "SET @myParam1 = :firstId ;\n"
				+ "SELECT o.id FROM SimpleEntity o WHERE o.id >= @myParam1 ORDER BY o.id;";

		final String query3 = "\n"
				+ "DECLARE @myParam1 VARCHAR(15);\n"
				+ "DECLARE @myParam2 VARCHAR(15);\n"
				+ "SET @myParam1 = :firstId ;\n"
				+ "SELECT o.id FROM SimpleEntity o WHERE o.id >= @myParam1 ORDER BY o.id  ;  ";

		final String query4 = "\n"
				+ "DECLARE @myParam1 VARCHAR(15);\n"
				+ "DECLARE @myParam2 VARCHAR(15);\n"
				+ "SET @myParam1 = :firstId ;\n"
				+ "SELECT o.id FROM SimpleEntity o\n"
				+ "WHERE o.id >= @myParam1 \n"
				+ "ORDER BY o.id    ";

		final String query5 = "\n"
				+ "DECLARE @myParam1 VARCHAR(15)\n"
				+ "DECLARE @myParam2 VARCHAR(15)\n"
				+ "SET @myParam1 = :firstId \n"
				+ "SELECT o.id FROM SimpleEntity o\n"
				+ "WHERE o.id >= @myParam1 \n"
				+ "ORDER BY o.id    ";

		final String query6 = "\n"
				+ "DECLARE @myParam1 VARCHAR(15)\n"
				+ "DECLARE @myParam2 VARCHAR(15)\n"
				+ "SET @myParam1 = :firstId \n"
				+ "SELECT o.id FROM SimpleEntity o\n"
				+ "WHERE o.id >= @myParam1 \n"
				+ "ORDER BY o.id ;   ";

		final String query7 = "\n"
				+ "DECLARE @myParam1 VARCHAR(15)\n"
				+ "DECLARE @myParam2 VARCHAR(15)\n"
				+ "SET @myParam1 = :firstId \n"
				+ "SELECT o.id FROM SimpleEntity o\n"
				+ "WHERE o.id >= @myParam1 \n"
				+ "ORDER BY o.id\n"
				+ " ;   ";

		// Verify the fix of SQLServerException: Incorrect syntax near 'offset'
		doInHibernate( this::entityManagerFactory, entityManager -> {
			for ( String query : List.of( query1, query2, query3, query4, query5, query6, query7 ) ) {
				final List<Integer> results = entityManager
						.createNativeQuery( query, Integer.class )
						.setParameter( "firstId", 10 )
						.setFirstResult( 0 )
						.setMaxResults( 5 )
						.getResultList();
				// verify that the paginated query returned the right ids.
				assertEquals( Arrays.asList( 10, 11, 12, 13, 14 ), results );
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-17700")
	public void testPaginatedNativeQueries() {
		// prepare some test data
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 1; i <= 20; ++i ) {
				final SimpleEntity entity = new SimpleEntity( i, "Entity" + i );
				entityManager.persist( entity );
			}
		} );

		final String query1 = "SELECT o.id FROM SimpleEntity o ORDER BY o.id";

		final String query2 = "SELECT o.id FROM SimpleEntity o ORDER BY o.id;";

		final String query3 = "SELECT o.id FROM SimpleEntity o ORDER BY o.id  ;  ";

		final String query4 = "SELECT o.id FROM SimpleEntity o\n" +
				"ORDER BY o.id    ";

		final String query5 = "SELECT o.id FROM SimpleEntity o\n" +
				"ORDER BY o.id  ;  ";

		final String query6 = "SELECT o.id FROM SimpleEntity o\n"
						+ "ORDER BY o.id\n"
						+ ";  ";

		// Verify the fix of SQLServerException: Incorrect syntax near 'offset'
		doInHibernate( this::entityManagerFactory, entityManager -> {
			for ( String query : List.of( query1, query2, query3, query4, query5, query6 ) ) {
				final List<Integer> results = entityManager
						.createNativeQuery( query, Integer.class )
						.setFirstResult( 2 )
						.setMaxResults( 5 )
						.getResultList();
				// verify that the paginated query returned the right ids.
				assertEquals( Arrays.asList( 3, 4, 5, 6, 7 ), results );
			}
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity implements Serializable {
		@Id
		private Integer id;
		private String name;

		SimpleEntity() {}

		SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
