/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedToOneTest.FilterTarget.class,
		RestrictedToOneTest.FilterFk.class, RestrictedToOneTest.FilterJoin.class })
@ServiceRegistry(settings = @Setting(name = "hibernate.cache.use_second_level_cache", value = "false"))
@SessionFactory(useCollectingStatementObserver = true)
class RestrictedToOneQueryOptimizationTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
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
						+ " o join o.target t where t.id is null", Long.class ).getResultList() ).isEmpty();
				inspector.clear();
				assertThat( session.createQuery( "select o.id from " + entity
						+ " o where fk(o.target) = 2", Long.class ).getResultList() ).containsExactly( 2L );
				assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContain( "join restricted_filter_target " );
			}
		} );
	}
}
