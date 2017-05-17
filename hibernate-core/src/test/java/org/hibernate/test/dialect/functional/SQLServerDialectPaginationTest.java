/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

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