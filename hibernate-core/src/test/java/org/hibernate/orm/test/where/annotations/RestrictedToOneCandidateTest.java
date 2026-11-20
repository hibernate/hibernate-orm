package org.hibernate.orm.test.where.annotations;

import java.util.stream.Stream;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class RestrictedToOneCandidateTest extends AbstractRestrictedToOneCandidateTest {
	@Test
	void lazyManyToOneRetainsAProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var owner = session.find( ManyLazy.class, 1L );
			assertThat( Hibernate.isInitialized( owner.getFiltered() ) ).isFalse();
			assertTarget( owner.getFiltered(), 102L );
		} );
	}

	@ParameterizedTest
	@MethodSource("representativeMappings")
	void correlatedNullnessUsesTheSelectedTarget(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final String query = "select o.id from " + type.getSimpleName()
					+ " o where exists (select 1 from CandidateLabel l where l.id=o.id and ";
			assertThat( session.createSelectionQuery( query + "o.filtered is null) order by o.id", Long.class )
					.getResultList() ).containsExactly( 2L, 4L );
			assertThat( session.createSelectionQuery( query + "o.filtered is not null) order by o.id", Long.class )
					.getResultList() ).containsExactly( 1L, 3L );
			assertThat( session.createSelectionQuery( query + "o.filtered is null and o.english is not null)",
					Long.class ).getResultList() ).containsExactly( 4L );
		} );
	}

	@Test
	void nativeQueryLoadsARepeatedKeyOncePerAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 5; id <= 9; id++ ) {
				session.createNativeMutationQuery( "insert into candidate_owner (id,label_id,name) values (:id,:label,'more')" )
						.setParameter( "id", id ).setParameter( "label", id <= 7 ? 1L : 2L ).executeUpdate();
			}
		} );
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			final var owners = session.createNativeQuery(
					"select * from candidate_owner where label_id in (1,2) order by label_id, id", ManyLazy.class ).getResultList();
			assertThat( owners ).hasSize( 7 );
			for ( var owner : owners.subList( 0, 4 ) ) {
				assertTarget( owner.getFiltered(), 102L );
				assertTarget( owner.getEnglish(), 101L );
				assertThat( owner.getFiltered() ).isSameAs( owners.get( 0 ).getFiltered() );
				assertThat( owner.getEnglish() ).isSameAs( owners.get( 0 ).getEnglish() );
			}
			for ( var owner : owners.subList( 4, 7 ) ) {
				assertTarget( owner.getFiltered(), null );
				assertTarget( owner.getEnglish(), null );
			}
			// The owners, then each association once for the key with targets and once for the key without
			assertThat( inspector.getSqlQueries() ).hasSize( 5 );
		} );
	}

	static Stream<Class<? extends Owner>> mappings() {
		return Stream.of(
				ManyLazy.class, ManyEager.class, ManySelect.class, ManySubselect.class,
				OneLazy.class, OneEager.class, OneSelect.class, OneSubselect.class,
				InverseLazy.class, InverseEager.class, InverseSelect.class, InverseSubselect.class
		);
	}

	static Stream<Arguments> loadingCases() {
		// Exercise mapped fetching through both entry points. Explicit fetch overrides
		// need one representative of each association kind, not every mapped strategy.
		return Stream.concat(
				mappings().flatMap( type -> Stream.of( "find", "hql" )
						.map( loading -> Arguments.of( type, loading ) ) ),
				Stream.concat(
						representativeMappings().flatMap( type -> Stream.of( "fetch", "graph", "native" )
								.map( loading -> Arguments.of( type, loading ) ) ),
						Stream.of( ManyEager.class, OneEager.class, InverseEager.class )
								.map( type -> Arguments.of( type, "graphLazy" ) )
				)
		);
	}

	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("loadingCases")
	void selectsTheMatchingTarget(Class<? extends Owner> type, String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final Owner owner = load( session, type, loading, 1L );
			assertTarget( owner.getFiltered(), 102L );
			assertTarget( owner.getEnglish(), 101L );
			assertThat( owner.getFiltered() ).isSameAs( session.find( Translation.class, 102L ) );
			final Owner absent = load( session, type, loading, 2L );
			assertTarget( absent.getFiltered(), null );
			assertTarget( absent.getEnglish(), null );
			final Owner hidden = load( session, type, loading, 3L );
			assertTarget( hidden.getFiltered(), 301L );
			assertTarget( hidden.getEnglish(), null );
			final Owner unique = load( session, type, loading, 4L );
			assertTarget( unique.getFiltered(), null );
			assertTarget( unique.getEnglish(), 401L );
		} );
	}

	static Stream<Arguments> visibilityCases() {
		return representativeMappings().flatMap( type -> Stream.of( "en", "nl", "fr" )
				.map( language -> Arguments.of( type, language ) ) );
	}

	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("visibilityCases")
	void restrictionsApplyToAlreadyManagedTargets(Class<? extends Owner> type, String language, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createSelectionQuery( "from CandidateTranslation", Translation.class ).getResultList();
			enable( session, language );
			final var owner = session.find( type, 1L );
			assertTarget( owner.getFiltered(), switch ( language ) {
				case "en" -> 101L;
				case "nl" -> 102L;
				default -> null;
			} );
			assertTarget( owner.getEnglish(), 101L );
			assertTarget( session.find( Translation.class, 102L ), 102L );
			if ( language.equals( "en" ) ) {
				assertThat( owner.getFiltered() ).isSameAs( owner.getEnglish() );
			}
		} );
	}

	static Stream<Class<? extends Owner>> batchMappings() {
		return Stream.of( ManyLazy.class, OneLazy.class, InverseLazy.class,
				ManySubselect.class, OneSubselect.class, InverseSubselect.class );
	}

	@ParameterizedTest
	@MethodSource("batchMappings")
	void batchLoadingKeepsAssociationIdentity(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			session.getLoadQueryInfluencers().setBatchSize( 8 );
			final var owners = session.createSelectionQuery( "from " + type.getSimpleName() + " o order by o.id", type ).getResultList();
			assertThat( owners ).hasSize( 4 );
			assertTarget( owners.get( 0 ).getFiltered(), 102L );
			assertTarget( owners.get( 0 ).getEnglish(), 101L );
			assertTarget( owners.get( 1 ).getFiltered(), null );
			assertTarget( owners.get( 2 ).getFiltered(), 301L );
			assertTarget( owners.get( 2 ).getEnglish(), null );
			assertTarget( owners.get( 3 ).getFiltered(), null );
			assertTarget( owners.get( 3 ).getEnglish(), 401L );
			final var targetDescriptor = session.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( Translation.class );
			assertThat( session.getPersistenceContextInternal().getEntity(
					session.generateEntityKey( 103L, targetDescriptor ) ) ).isNull();
		} );
	}

	@ParameterizedTest
	@MethodSource("representativeMappings")
	void hqlExpressionsUseTheSelectedTarget(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final String from = " from " + type.getSimpleName() + " o";
			assertThat( session.createSelectionQuery( "select id(o.filtered)" + from
					+ " where o.id=1", Long.class ).getSingleResult() ).isEqualTo( 102L );
			assertThat( session.createSelectionQuery( "select id(o.english)" + from
					+ " where o.id=1", Long.class ).getSingleResult() ).isEqualTo( 101L );
			assertThat( session.createSelectionQuery( "select count(o.filtered)" + from, Long.class )
					.getSingleResult() ).isEqualTo( 2L );
			assertThat( session.createSelectionQuery( "select o.filtered" + from
					+ " order by o.id", Translation.class ).getResultList() )
					.extracting( Translation::getId ).containsExactly( 102L, 301L );
			assertThat( session.createSelectionQuery( "select o.id" + from
					+ " where o.filtered is null order by o.id", Long.class ).getResultList() )
					.containsExactly( 2L, 4L );
			assertThat( session.createSelectionQuery( "select o.id" + from
					+ " where o.english is null order by o.id", Long.class ).getResultList() )
					.containsExactly( 2L, 3L );
			for ( long id : new long[] { 101, 102, 103 } ) {
				final var matchingOwners = session.createSelectionQuery( "select o.id" + from
						+ " where o.filtered=:target", Long.class )
						.setParameter( "target", session.find( Translation.class, id ) ).getResultList();
				if ( id == 102 ) {
					assertThat( matchingOwners ).containsExactly( 1L );
				}
				else {
					assertThat( matchingOwners ).isEmpty();
				}
				final var englishOwners = session.createSelectionQuery( "select o.id" + from
						+ " where o.english=:target", Long.class )
						.setParameter( "target", session.find( Translation.class, id ) ).getResultList();
				if ( id == 101 ) {
					assertThat( englishOwners ).containsExactly( 1L );
				}
				else {
					assertThat( englishOwners ).isEmpty();
				}
			}
		} );
	}

	@ParameterizedTest
	@MethodSource("representativeMappings")
	void fetchJoinSupportsOrderingAndPaging(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			scope.getCollectingStatementInspector().clear();
			final var owners = session.createSelectionQuery( "from " + type.getSimpleName()
					+ " o left join fetch o.filtered join fetch o.english e order by e.text", type )
					.setMaxResults( 2 ).getResultList();
			assertThat( owners ).extracting( o -> o.id ).containsExactly( 4L, 1L );
			assertTarget( owners.get( 0 ).getFiltered(), null );
			assertTarget( owners.get( 1 ).getFiltered(), 102L );
			assertThat( scope.getCollectingStatementInspector().getSqlQueries() ).hasSize( 1 );
		} );
	}

	@ParameterizedTest
	@MethodSource("representativeMappings")
	void disabledFilterWithUniqueDataStillWorks(Class<? extends Owner> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Owner owner = session.find( type, 4L );
			assertTarget( owner.getFiltered(), 401L );
			assertTarget( owner.getEnglish(), 401L );
		} );
	}

}
