/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.sqm.mutation.internal.inline.InlineStrategy;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13214")
public class InheritanceDeleteBatchTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TestEntity.class,
				TestEntityType1.class,
				TestEntityType2.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(
				AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
				InlineStrategy.class.getName()
		);
		configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			session.persist( new TestEntity( 1 ) );
			session.persist( new TestEntityType1( 2 ) );
			session.persist( new TestEntityType2( 3 ) );
			session.persist( new TestEntityType2( 4 ) );
		} );
	}

	@Test
	public void testDelete() {
		StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 1; i <= 4; i++ ) {
				Query deleteQuery = session.createQuery( "delete TestEntity e where e.id = :id" );
				deleteQuery.setParameter( "id", i );
				deleteQuery.executeUpdate();
				assertThat( statistics.getPrepareStatementCount(), is( 4L ) );
				statistics.clear();
			}
		} );
	}

	@Entity(name = "TestEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		int id;

		private String field;

		public TestEntity() {
		}

		public TestEntity(int id) {
			this.id = id;
		}
	}

	@Entity(name = "TestEntityType1")
	@Table(name = "test_entity_type1")
	public static class TestEntityType1 extends TestEntity {

		public TestEntityType1(int id) {
			super( id );
		}
	}

	@Entity(name = "TestEntityType2")
	@Table(name = "test_entity_type2")
	public static class TestEntityType2 extends TestEntity {
		public TestEntityType2(int id) {
			super( id );
		}
	}
}
