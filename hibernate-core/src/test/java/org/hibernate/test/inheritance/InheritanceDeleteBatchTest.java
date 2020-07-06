/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Query;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.hql.spi.id.inline.InlineIdsInClauseBulkIdStrategy;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
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
@RequiresDialectFeature(DialectChecks.SupportsRowValueConstructorSyntaxInInListCheck.class)
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
				AvailableSettings.HQL_BULK_ID_STRATEGY,
				InlineIdsInClauseBulkIdStrategy.class.getName()
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
