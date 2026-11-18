package org.hibernate.orm.test.where.annotations;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.hibernate.Hibernate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RestrictedToOneCandidateMutationTest extends AbstractRestrictedToOneCandidateTest {
	@ParameterizedTest
	@MethodSource("representativeMappings")
	void changingFilterRequiresRefresh(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "en" );
			final Owner owner = session.find( type, 1L );
			assertTarget( owner.getFiltered(), 101L );
			enable( session, "nl" );
			assertTarget( owner.getFiltered(), 101L );
			session.refresh( owner );
			assertTarget( owner.getFiltered(), 102L );
			assertTarget( owner.getEnglish(), 101L );
		} );
	}

	@ParameterizedTest
	@MethodSource("representativeMappings")
	void updateAndMergePreserveHiddenPhysicalKey(Class<? extends Owner> type, SessionFactoryScope scope) {
		final Owner detached = scope.fromTransaction( session -> {
			enable( session, "fr" );
			final Owner owner = session.find( type, 3L );
			assertTarget( owner.getFiltered(), null );
			assertTarget( owner.getEnglish(), null );
			owner.name = "updated";
			return owner;
		} );
		detached.name = "merged";
		scope.inTransaction( session -> {
			enable( session, "fr" );
			session.merge( detached );
		} );
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select label_id from candidate_owner where id=3", Long.class ).getSingleResult() )
					.isEqualTo( 3L );
			assertThat( session.createNativeQuery( "select count(*) from candidate_translation where label_id=3", Long.class ).getSingleResult() )
					.isEqualTo( 1L );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void mergeRetainsSelectedTargetIdentity(boolean initialize, SessionFactoryScope scope) {
		final var detached = scope.fromTransaction( session -> {
			enable( session, "nl" );
			final var owner = session.find( ManyLazy.class, 1L );
			if ( initialize ) {
				assertTarget( owner.getFiltered(), 102L );
				assertTarget( owner.getEnglish(), 101L );
			}
			else {
				assertThat( Hibernate.isInitialized( owner.getFiltered() ) ).isFalse();
				assertThat( Hibernate.isInitialized( owner.getEnglish() ) ).isFalse();
			}
			return owner;
		} );
		detached.name = "changed";
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var merged = session.merge( detached );
			assertTarget( merged.getFiltered(), 102L );
			assertTarget( merged.getEnglish(), 101L );
		} );
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var owner = session.find( ManyLazy.class, 1L );
			assertThat( owner.name ).isEqualTo( "changed" );
			assertTarget( owner.getFiltered(), 102L );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	void nativeLoadRetainsForeignKeyWhenTheSelectedTargetDisappears(SessionFactoryScope scope) {
		final var deleted = new AtomicBoolean();
		// Remove the selected target after the identifier lookup, just before loading its state.
		final UnaryOperator<String> deleteBeforeLoad = sql -> {
			if ( sql.startsWith( "select" ) && sql.contains( "candidate_translation" )
					&& sql.contains( "target_text" ) && deleted.compareAndSet( false, true ) ) {
				scope.inTransaction( other -> other.createNativeMutationQuery(
						"delete from candidate_translation where id=301" ).executeUpdate() );
			}
			return sql;
		};
		scope.inTransaction( factory -> factory.withOptions().statementInspector( deleteBeforeLoad ).openSession(),
				session -> {
					enable( session, "nl" );
					final var owner = load( session, ManySelect.class, "native", 3L );
					assertThat( deleted.get() ).isTrue();
					assertThat( owner.getFiltered() ).isNull();
					owner.name = "changed";
				} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select label_id, name from candidate_owner where id=3", Object[].class ).getSingleResult() )
				.containsExactly( 3L, "changed" ) );
	}

	@ParameterizedTest
	@MethodSource("owningMappings")
	void replacingTheTargetWritesThePhysicalKey(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "fr" );
			final Owner owner = session.find( type, 1L );
			assertTarget( owner.getFiltered(), null );
			owner.setFiltered( session.find( Translation.class, 401L ) );
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select label_id from candidate_owner where id=1", Long.class ).getSingleResult() ).isEqualTo( 4L ) );
	}


	static Stream<Class<? extends Owner>> owningMappings() {
		return Stream.of( ManyLazy.class, OneLazy.class );
	}

	static Stream<Class<? extends Owner>> oneToOneMappings() {
		return Stream.of( OneLazy.class, InverseLazy.class );
	}

	@ParameterizedTest
	@MethodSource("oneToOneMappings")
	void orphanRemovalDeletesOnlyTheSelectedCandidate(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var owner = session.find( type, 1L );
			assertTarget( owner.getFiltered(), 102L );
			owner.setFiltered( null );
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select id from candidate_translation where label_id=1 order by id", Long.class ).getResultList() )
				.containsExactly( 101L, 103L ) );
	}

}
