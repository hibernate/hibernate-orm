package org.hibernate.orm.test.where.annotations;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.JoinType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DomainModel(annotatedClasses = { RestrictedToOneTest.FilterTarget.class,
		RestrictedToOneTest.FilterFk.class, RestrictedToOneTest.FilterJoin.class,
		RestrictedToOneTest.SqlTarget.class, RestrictedToOneTest.SqlFk.class, RestrictedToOneTest.SqlJoin.class,
		RestrictedToOneQueryOptimizationTest.Lookup.class })
@ServiceRegistry(settings = @Setting(name = "hibernate.cache.use_second_level_cache", value = "false"))
@SessionFactory(useCollectingStatementObserver = true)
class RestrictedToOneQueryOptimizationTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 2; id++ ) {
				final var lookup = new Lookup();
				lookup.id = id;
				lookup.active = id == 1;
				session.persist( lookup );
			}
			for ( long id = 1; id <= 3; id++ ) {
				final var target = new RestrictedToOneTest.FilterTarget();
				target.id = id;
				target.active = id == 1;
				session.persist( target );
				final var fk = new RestrictedToOneTest.FilterFk();
				fk.id = id;
				fk.setTarget( id == 3 ? null : target );
				session.persist( fk );
				final var join = new RestrictedToOneTest.FilterJoin();
				join.id = id;
				join.setTarget( id == 3 ? null : target );
				session.persist( join );
				final var sqlTarget = new RestrictedToOneTest.SqlTarget();
				sqlTarget.id = id;
				sqlTarget.active = id == 1;
				session.persist( sqlTarget );
				final var sqlFk = new RestrictedToOneTest.SqlFk();
				sqlFk.id = id;
				sqlFk.setTarget( id == 3 ? null : sqlTarget );
				session.persist( sqlFk );
				final var sqlJoin = new RestrictedToOneTest.SqlJoin();
				sqlJoin.id = id;
				sqlJoin.setTarget( id == 3 ? null : sqlTarget );
				session.persist( sqlJoin );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@ParameterizedTest
	@ValueSource(classes = { RestrictedToOneTest.FilterFk.class, RestrictedToOneTest.FilterJoin.class,
			RestrictedToOneTest.SqlFk.class, RestrictedToOneTest.SqlJoin.class })
	void criteriaOnClausePreservesOwners(Class<? extends RestrictedToOneTest.Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( boolean enabled : new boolean[] { false, true } ) {
				if ( enabled ) {
					RestrictedToOneTest.enable( session );
				}
				else {
					session.disableFilter( "visibleTarget" );
				}
				final boolean restricted = enabled
						|| type == RestrictedToOneTest.SqlFk.class || type == RestrictedToOneTest.SqlJoin.class;
				for ( boolean explicitJoin : new boolean[] { false, true } ) {
					final var cb = session.getCriteriaBuilder();
					final var query = cb.createTupleQuery();
					final var root = query.from( type );
					if ( explicitJoin ) {
						root.join( "target", JoinType.LEFT );
					}
					final var other = root.join( Lookup.class, JoinType.LEFT );
					other.on( cb.equal( other.get( "active" ), root.get( "target" ).get( "active" ) ) );
					query.select( cb.tuple( root.get( "id" ), other.get( "id" ) ) ).orderBy( cb.asc( root.get( "id" ) ) );
					assertThat( session.createQuery( query ).getResultList() )
							.as( "%s, filter enabled=%s, explicit join=%s", type.getSimpleName(), enabled, explicitJoin )
							.extracting( row -> row.get( 0, Long.class ), row -> row.get( 1, Long.class ) )
							.containsExactly( tuple( 1L, 1L ), tuple( 2L, restricted ? null : 2L ), tuple( 3L, null ) );
				}
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = { RestrictedToOneTest.FilterFk.class, RestrictedToOneTest.FilterJoin.class,
			RestrictedToOneTest.SqlFk.class, RestrictedToOneTest.SqlJoin.class })
	void criteriaNullnessUsesRestrictedTarget(Class<? extends RestrictedToOneTest.Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( boolean enabled : new boolean[] { false, true } ) {
				if ( enabled ) {
					RestrictedToOneTest.enable( session );
				}
				else {
					session.disableFilter( "visibleTarget" );
				}
				final boolean restricted = enabled
						|| type == RestrictedToOneTest.SqlFk.class || type == RestrictedToOneTest.SqlJoin.class;
				for ( boolean explicitJoin : new boolean[] { false, true } ) {
					final var cb = session.getCriteriaBuilder();
					final var query = cb.createQuery( Long.class );
					final var root = query.from( type );
					if ( explicitJoin ) {
						root.join( "target", JoinType.LEFT );
					}
					query.select( root.get( "id" ) ).where( cb.isNull( root.get( "target" ) ) )
							.orderBy( cb.asc( root.get( "id" ) ) );
					assertThat( session.createQuery( query ).getResultList() )
							.as( "%s, filter enabled=%s, explicit join=%s", type.getSimpleName(), enabled, explicitJoin )
							.containsExactly( restricted ? new Long[] { 2L, 3L } : new Long[] { 3L } );
				}
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "FilterFk", "FilterJoin" })
	void targetIdJoinsOnlyWhileFilterEnabled(String entity, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var inspector = scope.getCollectingStatementObserver();
			for ( boolean enabled : new boolean[] { false, true, false } ) {
				if ( enabled ) {
					RestrictedToOneTest.enable( session );
				}
				else {
					session.disableFilter( "visibleTarget" );
				}
				for ( String query : new String[] {
						"select o.target.id from " + entity + " o where o.id = 2",
						"select o.id from " + entity + " o where o.target.id = 2" } ) {
					inspector.clear();
					final var results = session.createQuery( query, Long.class ).getResultList();
					assertThat( results ).containsExactly( enabled ? new Long[0] : new Long[] { 2L } );
					assertThat( inspector.getSqlQueries() ).hasSize( 1 );
					assertThat( inspector.getSqlQueries().get( 0 ).contains( "join restricted_filter_target " ) )
							.isEqualTo( enabled );
				}
				final var target = new RestrictedToOneTest.FilterTarget();
				target.id = 2L;
				assertThat( session.createQuery( "select o.id from " + entity + " o where o.target = :target", Long.class )
						.setParameter( "target", target ).getResultList() )
						.containsExactly( enabled ? new Long[0] : new Long[] { 2L } );
				assertThat( session.createQuery( "select o.id from " + entity
						+ " o left join o.target t where t.id is null order by o.id", Long.class ).getResultList() )
						.containsExactly( enabled ? new Long[] { 2L, 3L } : new Long[] { 3L } );
				assertThat( session.createQuery( "select o.id from " + entity
						+ " o where o.target is null order by o.id", Long.class ).getResultList() )
						.containsExactly( enabled ? new Long[] { 2L, 3L } : new Long[] { 3L } );
				assertThat( session.createQuery( "select o.id from " + entity
						+ " o join o.target t where t.id is null", Long.class ).getResultList() ).isEmpty();
				inspector.clear();
				assertThat( session.createQuery( "select o.id from " + entity
						+ " o where fk(o.target) = 2", Long.class ).getResultList() ).containsExactly( 2L );
				assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContain( "join restricted_filter_target " );
			}
		} );
	}

	@Entity(name = "RestrictedToOneLookup")
	@Table(name = "restricted_to_one_lookup")
	static class Lookup {
		@Id Long id;
		boolean active;
	}
}
