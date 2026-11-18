package org.hibernate.orm.test.where.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/** Entity restrictions alone select a target from rows sharing a nonunique join key. */
@JiraKey("HHH-21131")
@DomainModel(annotatedClasses = {
		RestrictedToOneEntityCandidateTest.Owner.class,
		RestrictedToOneEntityCandidateTest.SqlCandidate.class,
		RestrictedToOneEntityCandidateTest.FilterCandidate.class
})
@SessionFactory(exportSchema = false, useCollectingStatementObserver = true)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = {
		RestrictedToOneEntityCandidateTest.Owner.class,
		RestrictedToOneEntityCandidateTest.SqlCandidate.class,
		RestrictedToOneEntityCandidateTest.FilterCandidate.class
})
class RestrictedToOneEntityCandidateTest {
	private RestrictedToOneCandidateSchema schema;

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		schema = new RestrictedToOneCandidateSchema( scope,
				RestrictedToOneCandidateSchema.EntityOwner.class, RestrictedToOneCandidateSchema.EntityTarget.class );
		scope.inTransaction( session -> {
			schema.insert( session, "entity_candidate_owner", "id,label_code,name",
					new Object[] { 1, "root", "one" },
					new Object[] { 2, "hidden", "two" },
					new Object[] { 3, null, "three" },
					new Object[] { 4, "en-only", "four" },
					new Object[] { 5, "root", "five" } );
			schema.insert( session, "entity_candidate_target", "id,label_code,lang_code,target_text,next_code",
					new Object[] { 101, "root", "en", "English root", "middle" },
					new Object[] { 102, "root", "nl", "Dutch root", "middle" },
					new Object[] { 201, "middle", "en", "English middle", "leaf" },
					new Object[] { 202, "middle", "nl", "Dutch middle", "leaf" },
					new Object[] { 301, "leaf", "en", "English leaf", "root" },
					new Object[] { 302, "leaf", "nl", "Dutch leaf", "root" },
					new Object[] { 401, "hidden", "de", "Hidden", null },
					new Object[] { 501, "en-only", "en", "English only", null },
					new Object[] { 601, "hidden-root", "en", "English root", "hidden-middle" },
					new Object[] { 602, "hidden-root", "nl", "Dutch root", "hidden-middle" },
					new Object[] { 701, "hidden-middle", "en", "English middle", "hidden-leaf" },
					new Object[] { 702, "hidden-middle", "nl", "Dutch middle", "hidden-leaf" },
					new Object[] { 801, "hidden-leaf", "de", "Hidden leaf", null } );
		} );
	}

	@AfterEach
	void dropSchema() {
		if ( schema != null ) {
			schema.close();
		}
	}

	@Test
	void findSelectsTheTarget(SessionFactoryScope scope) {
		entityRestrictionsSelectTheTarget( "find", scope );
	}

	@Test
	void hqlSelectsTheTarget(SessionFactoryScope scope) {
		entityRestrictionsSelectTheTarget( "hql", scope );
	}

	@Test
	void fetchJoinSelectsTheTarget(SessionFactoryScope scope) {
		entityRestrictionsSelectTheTarget( "fetch", scope );
	}

	@Test
	void nativeQuerySelectsTheTarget(SessionFactoryScope scope) {
		entityRestrictionsSelectTheTarget( "native", scope );
	}

	@Test
	void nativeNewOwnersUseChangedEntityFilter(SessionFactoryScope scope) {
		newOwnersUseChangedEntityFilter( scope, true, false );
	}

	@Test
	void hqlNewOwnersUseChangedEntityFilter(SessionFactoryScope scope) {
		newOwnersUseChangedEntityFilter( scope, false, false );
	}

	@Test
	void nativeNewOwnersUseMutatedFilterArgument(SessionFactoryScope scope) {
		newOwnersUseChangedEntityFilter( scope, true, true );
	}

	private void newOwnersUseChangedEntityFilter(SessionFactoryScope scope, boolean nativeQuery, boolean listArgument) {
		scope.inTransaction( session -> {
			for ( int id = 6; id <= 10; id++ ) {
				schema.insert( session, "entity_candidate_owner", "id,label_code,name",
						new Object[] { id, "root", "extra" } );
			}
		} );
		scope.inTransaction( session -> {
			final var languages = new ArrayList<>( List.of( "en" ) );
			final var filter = session.enableFilter( "entityCandidateLanguage" );
			if ( listArgument ) {
				filter.setParameterList( "language", languages );
			}
			else {
				filter.setParameter( "language", "en" );
			}
			final var first = nativeOwner( session, 1 );
			assertTarget( first.getFiltered(), 101L );
			if ( listArgument ) {
				languages.set( 0, "nl" );
			}
			else {
				filter.setParameter( "language", "nl" );
			}
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			final var owners = nativeQuery
					? session.createNativeQuery( "select * from entity_candidate_owner where id between 5 and 9 order by id",
							Owner.class ).getResultList()
					: session.createSelectionQuery( "from EntityCandidateOwner where id between 5 and 9 order by id",
							Owner.class ).getResultList();
			assertThat( owners ).hasSize( 5 );
			for ( var owner : owners ) {
				assertTarget( owner.getFiltered(), 102L );
				assertThat( owner.getFiltered() ).isSameAs( owners.get( 0 ).getFiltered() );
			}
			assertTarget( first.getFiltered(), 101L );
			// One owner query and one target load, shared by all five new owners.
			assertThat( observer.getSqlQueries() ).hasSize( 2 );
			observer.clear();
			assertTarget( nativeOwner( session, 10 ).getFiltered(), 102L );
			// HQL loads by identifier and need not populate the unique-key cache.
			assertThat( observer.getSqlQueries() ).hasSize( nativeQuery ? 1 : 2 );
		} );
	}

	@Test
	void nativeFilterChangeCanExcludeCachedCandidate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "en" );
			assertTarget( nativeOwner( session, 1 ).getFiltered(), 101L );
			enable( session, "fr" );
			final var hidden = nativeOwner( session, 5 );
			assertThat( hidden.getFiltered() ).isNull();
			hidden.name = "updated";
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select label_code from entity_candidate_owner where id=5", String.class ).getSingleResult() )
				.isEqualTo( "root" ) );
	}

	@Test
	void refreshUsesChangedEntityFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "en" );
			final var owner = nativeOwner( session, 1 );
			assertTarget( owner.getFiltered(), 101L );
			enable( session, "nl" );
			session.refresh( owner );
			assertTarget( owner.getFiltered(), 102L );
		} );
	}

	private static Owner nativeOwner(Session session, long id) {
		return session.createNativeQuery( "select * from entity_candidate_owner where id=:id", Owner.class )
				.setParameter( "id", id ).getSingleResult();
	}

	private void entityRestrictionsSelectTheTarget(String loading, SessionFactoryScope scope) {
		for ( String language : new String[] { "en", "nl", "fr" } ) {
			assertSelectedTargets( loading, language, scope );
		}
	}

	private void assertSelectedTargets(String loading, String language, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, language );
			final var owners = loadOwners( session, loading );
			assertThat( owners ).hasSize( 5 );
			for ( var owner : owners ) {
				final Long englishId = switch ( owner.id.intValue() ) {
					case 1, 5 -> 101L;
					case 4 -> 501L;
					default -> null;
				};
				final Long filteredId = language.equals( "en" ) ? englishId
						: language.equals( "nl" ) && ( owner.id == 1L || owner.id == 5L ) ? 102L : null;
				assertTarget( owner.getEnglish(), englishId );
				assertTarget( owner.getFiltered(), filteredId );
				owner.name = "updated";
			}
			assertThat( owners.get( 4 ).getEnglish() ).isSameAs( owners.get( 0 ).getEnglish() );
			assertThat( owners.get( 4 ).getFiltered() ).isSameAs( owners.get( 0 ).getFiltered() );
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select label_code from entity_candidate_owner order by id", String.class ).getResultList() )
				.containsExactly( "root", "hidden", null, "en-only", "root" ) );
	}

	@Test
	void entityRestrictionsDetermineHqlNullness(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			assertThat( session.createSelectionQuery(
					"select o.id from EntityCandidateOwner o where o.filtered is null order by o.id", Long.class )
					.getResultList() ).containsExactly( 2L, 3L, 4L );
			assertThat( session.createSelectionQuery(
					"select o.id from EntityCandidateOwner o where o.english is null order by o.id", Long.class )
					.getResultList() ).containsExactly( 2L, 3L );
		} );
	}

	@Test
	void circularSqlRestrictionRetainsTheSelectedLazyTarget(SessionFactoryScope scope) {
		circularFetchRetainsTheSelectedLazyTarget( SqlCandidate.class, scope );
	}

	@Test
	void circularFilterRetainsTheSelectedLazyTarget(SessionFactoryScope scope) {
		circularFetchRetainsTheSelectedLazyTarget( FilterCandidate.class, scope );
	}

	private void circularFetchRetainsTheSelectedLazyTarget(Class<? extends Candidate> type, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			final var root = session.createSelectionQuery( "from " + type.getSimpleName()
					+ " t join fetch t.next where t.labelCode='root'", type ).getSingleResult();
			final long offset = type == SqlCandidate.class ? 1 : 2;
			assertTarget( root, 100L + offset );
			assertTarget( root.getNext(), 200L + offset );
			final var leaf = root.getNext().getNext();
			assertThat( Hibernate.isInitialized( leaf ) ).isFalse();
			assertTarget( leaf, 300L + offset );
			assertTarget( leaf.getNext(), 100L + offset );
			assertThat( leaf.getNext() ).isSameAs( root );
			assertThat( observer.getSqlQueries() ).hasSize( 2 );
		} );
	}

	@Test
	void circularSqlRestrictionRetainsTheHiddenPhysicalKey(SessionFactoryScope scope) {
		circularFetchRetainsTheHiddenPhysicalKey( SqlCandidate.class, scope );
	}

	@Test
	void circularFilterRetainsTheHiddenPhysicalKey(SessionFactoryScope scope) {
		circularFetchRetainsTheHiddenPhysicalKey( FilterCandidate.class, scope );
	}

	private void circularFetchRetainsTheHiddenPhysicalKey(Class<? extends Candidate> type, SessionFactoryScope scope) {
		final long middleId = type == SqlCandidate.class ? 701L : 702L;
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var root = session.createSelectionQuery( "from " + type.getSimpleName()
					+ " t join fetch t.next where t.labelCode='hidden-root'", type ).getSingleResult();
			final var middle = root.getNext();
			assertTarget( middle, middleId );
			assertThat( middle.getNext() ).isNull();
			middle.text = "updated";
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select next_code from entity_candidate_target where id=:id", String.class )
				.setParameter( "id", middleId ).getSingleResult() ).isEqualTo( "hidden-leaf" ) );
	}

	private static void enable(Session session, String language) {
		session.enableFilter( "entityCandidateLanguage" ).setParameter( "language", language );
	}

	private static List<Owner> loadOwners(Session session, String loading) {
		return switch ( loading ) {
			case "find" -> LongStream.rangeClosed( 1, 5 ).mapToObj( id -> session.find( Owner.class, id ) ).toList();
			case "native" -> session.createNativeQuery( "select * from entity_candidate_owner order by id", Owner.class )
					.getResultList();
			default -> session.createSelectionQuery( "from EntityCandidateOwner o"
					+ ( loading.equals( "fetch" ) ? " left join fetch o.filtered left join fetch o.english" : "" )
					+ " order by o.id", Owner.class ).getResultList();
		};
	}

	private static void assertTarget(Candidate target, Long id) {
		if ( id == null ) {
			assertThat( target ).isNull();
		}
		else {
			assertThat( target ).isNotNull();
			assertThat( target.getId() ).isEqualTo( id );
			assertThat( target.getText() ).isNotNull();
		}
	}

	@MappedSuperclass
	public abstract static class Candidate {
		@Id Long id;
		@Column(name = "label_code") String labelCode;
		@Column(name = "lang_code") String language;
		@Column(name = "target_text") String text;
		public Long getId() { return id; }
		public String getText() { return text; }
		public abstract Candidate getNext();
	}

	@Entity(name = "SqlCandidate")
	@Table(name = "entity_candidate_target")
	@SQLRestriction("lang_code = 'en'")
	public static class SqlCandidate extends Candidate {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "next_code", referencedColumnName = "label_code")
		SqlCandidate next;
		@Override public SqlCandidate getNext() { return next; }
	}

	@Entity(name = "FilterCandidate")
	@Table(name = "entity_candidate_target")
	@FilterDef(name = "entityCandidateLanguage", applyToLoadByKey = true,
			parameters = @ParamDef(name = "language", type = String.class))
	@Filter(name = "entityCandidateLanguage", condition = "lang_code in (:language)")
	public static class FilterCandidate extends Candidate {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "next_code", referencedColumnName = "label_code")
		FilterCandidate next;
		@Override public FilterCandidate getNext() { return next; }
	}

	@Entity(name = "EntityCandidateOwner")
	@Table(name = "entity_candidate_owner")
	public static class Owner {
		@Id Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_code", referencedColumnName = "label_code")
		FilterCandidate filtered;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "label_code", referencedColumnName = "label_code", insertable = false, updatable = false)
		SqlCandidate english;
		public FilterCandidate getFiltered() { return filtered; }
		public SqlCandidate getEnglish() { return english; }
	}
}
