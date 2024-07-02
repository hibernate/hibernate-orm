/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.querycache;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.query.Query;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		SingleTableInheritanceQueryCacheTest.AbstractEntity.class,
		SingleTableInheritanceQueryCacheTest.EntityA.class,
		SingleTableInheritanceQueryCacheTest.SubEntityA.class,
		SingleTableInheritanceQueryCacheTest.EntityB.class,
} )
@SessionFactory
@ServiceRegistry( settings = {
		@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "true" ),
		@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18017" )
public class SingleTableInheritanceQueryCacheTest {
	@Test
	public void testHQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> executeQuery( session, session.createQuery(
				"from AbstractEntity where id = 1",
				AbstractEntity.class
		), SubEntityA.class ) );
	}

	@Test
	public void testCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<AbstractEntity> query = cb.createQuery( AbstractEntity.class );
			final Root<AbstractEntity> from = query.from( AbstractEntity.class );
			executeQuery(
					session,
					session.createQuery( query.where( cb.equal( from.get( "id" ), 2 ) ) ),
					EntityB.class
			);
		} );
	}

	private void executeQuery(
			SessionImplementor session,
			Query<AbstractEntity> query,
			Class<? extends AbstractEntity> resultClass) {
		final StatisticsImplementor statistics = session.getSessionFactory().getStatistics();
		query.setHint( HibernateHints.HINT_CACHEABLE, true );
		for ( int i = 0; i < 2; i++ ) {
			statistics.clear();
			session.clear();
			final AbstractEntity result = query.getSingleResult();
			assertThat( result ).isExactlyInstanceOf( resultClass );
			assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( i );
			assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 - i );
		}
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SubEntityA( 1L, "a1", 1 ) );
			session.persist( new EntityB( 2L, 2.0 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from AbstractEntity" ).executeUpdate() );
	}

	@Entity( name = "AbstractEntity" )
	@DiscriminatorColumn( name = "disc_col", discriminatorType = DiscriminatorType.INTEGER )
	static abstract class AbstractEntity {
		@Id
		private Long id;

		public AbstractEntity() {
		}

		public AbstractEntity(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "EntityA" )
	@DiscriminatorValue( "1" )
	static class EntityA extends AbstractEntity {
		private String aProp;

		public EntityA() {
		}

		public EntityA(Long id, String aProp) {
			super( id );
			this.aProp = aProp;
		}
	}

	@Entity( name = "SubEntityA" )
	@DiscriminatorValue( "11" )
	static class SubEntityA extends EntityA {
		private Integer subAProp;

		public SubEntityA() {
		}

		public SubEntityA(Long id, String aProp, Integer subAProp) {
			super( id, aProp );
			this.subAProp = subAProp;
		}
	}

	@Entity( name = "EntityB" )
	@DiscriminatorValue( "2" )
	static class EntityB extends AbstractEntity {
		private Double bProp;

		public EntityB() {
		}

		public EntityB(Long id, Double bProp) {
			super( id );
			this.bProp = bProp;
		}
	}
}
