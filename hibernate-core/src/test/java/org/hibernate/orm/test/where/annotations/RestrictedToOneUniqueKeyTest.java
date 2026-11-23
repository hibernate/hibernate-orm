package org.hibernate.orm.test.where.annotations;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedToOneUniqueKeyTest.SqlTarget.class,
		RestrictedToOneUniqueKeyTest.FilterTarget.class, RestrictedToOneUniqueKeyTest.Owner.class })
@SessionFactory(useCollectingStatementObserver = true)
class RestrictedToOneUniqueKeyTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@ParameterizedTest
	@CsvSource({ "native,false", "native,true", "hql,false", "hql,true" })
	void repeatedReferencesReuseRestrictedTargets(String loading, boolean alreadyLoaded, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var sql = sqlTarget( 20L );
			final var filter = filterTarget( 30L );
			session.persist( sql );
			session.persist( filter );
			for ( long id = 1; id <= 6; id++ ) {
				persistOwner( session, id, sql, filter );
			}
		} );
		scope.inTransaction( session -> {
			session.enableFilter( "uniqueActive" );
			if ( alreadyLoaded ) {
				// Populate both the identifier and unique-key entries before loading other owners.
				session.createNativeQuery( "select * from restricted_unique where id=1", Owner.class )
						.getSingleResult();
			}
			final var inspector = scope.getCollectingStatementObserver();
			inspector.clear();
			final var owners = loading.equals( "native" )
					? session.createNativeQuery( "select * from restricted_unique where id>1 order by id", Owner.class )
							.getResultList()
					: session.createSelectionQuery( "from UniqueOwner where id>1 order by id", Owner.class )
							.getResultList();
			assertThat( owners ).hasSize( 5 );
			for ( var owner : owners ) {
				Hibernate.initialize( owner.sql );
				Hibernate.initialize( owner.filter );
				assertThat( owner.sql ).isSameAs( owners.get( 0 ).sql );
				assertThat( owner.filter ).isSameAs( owners.get( 0 ).filter );
				assertThat( Hibernate.unproxy( owner.sql, SqlTarget.class ).active ).isTrue();
				assertThat( Hibernate.unproxy( owner.filter, FilterTarget.class ).active ).isTrue();
			}
			assertThat( inspector.getSqlQueries() ).hasSize( alreadyLoaded ? 1 : 3 );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "native", "hql" })
	void distinctTargetsNeedOnlyOneSelectEach(String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 5; id++ ) {
				final var sql = sqlTarget( id );
				final var filter = filterTarget( id );
				session.persist( sql );
				session.persist( filter );
				persistOwner( session, id, sql, filter );
			}
		} );
		scope.inTransaction( session -> {
			session.enableFilter( "uniqueActive" );
			final var inspector = scope.getCollectingStatementObserver();
			inspector.clear();
			final var owners = loading.equals( "native" )
					? session.createNativeQuery( "select * from restricted_unique order by id", Owner.class ).getResultList()
					: session.createSelectionQuery( "from UniqueOwner order by id", Owner.class ).getResultList();
			assertThat( owners ).hasSize( 5 );
			for ( var owner : owners ) {
				Hibernate.initialize( owner.sql );
				Hibernate.initialize( owner.filter );
				assertThat( Hibernate.unproxy( owner.sql, SqlTarget.class ).active ).isTrue();
				assertThat( Hibernate.unproxy( owner.filter, FilterTarget.class ).active ).isTrue();
			}
			assertThat( inspector.getSqlQueries() ).hasSize( 11 );
		} );
	}

	@Test
	void enablingAndDisablingEntityFilterInvalidatesUniqueKeyResolution(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var sql = sqlTarget( 20L );
			final var filter = filterTarget( 30L );
			filter.active = false;
			session.persist( sql );
			session.persist( filter );
			for ( long id = 1; id <= 4; id++ ) {
				persistOwner( session, id, sql, filter );
			}
		} );
		scope.inTransaction( session -> {
			final var first = session.createNativeQuery(
					"select * from restricted_unique where id=1", Owner.class ).getSingleResult();
			assertThat( first.filter ).isNotNull();
			session.enableFilter( "uniqueActive" );
			final var hidden = session.createNativeQuery(
					"select * from restricted_unique where id=2", Owner.class ).getSingleResult();
			assertThat( hidden.filter ).isNull();
			assertThat( first.filter ).isNotNull();
			session.disableFilter( "uniqueActive" );
			final var visible = session.createNativeQuery(
					"select * from restricted_unique where id=3", Owner.class ).getSingleResult();
			assertThat( visible.filter ).isSameAs( first.filter );
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			final var last = session.createNativeQuery(
					"select * from restricted_unique where id=4", Owner.class ).getSingleResult();
			assertThat( last.filter ).isSameAs( first.filter );
			assertThat( observer.getSqlQueries() ).hasSize( 1 );
		} );
	}

	private static SqlTarget sqlTarget(long id) {
		final var target = new SqlTarget();
		target.id = id;
		target.code = "sql-" + id;
		target.active = true;
		return target;
	}

	private static FilterTarget filterTarget(long id) {
		final var target = new FilterTarget();
		target.id = id;
		target.code = "filter-" + id;
		target.active = true;
		return target;
	}

	private static void persistOwner(Session session, long id, SqlTarget sql, FilterTarget filter) {
		final var owner = new Owner();
		owner.id = id;
		owner.sql = sql;
		owner.filter = filter;
		session.persist( owner );
	}

	@ParameterizedTest
	@ValueSource(strings = { "find", "fetch", "native" })
	void storedUniqueKeysArePreserved(String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var sql = new SqlTarget();
			sql.id = 20L;
			sql.code = "hidden-sql";
			final var filter = new FilterTarget();
			filter.id = 30L;
			filter.code = "hidden-filter";
			session.persist( sql );
			session.persist( filter );
			final var owner = new Owner();
			owner.id = 1L;
			owner.sql = sql;
			owner.filter = filter;
			session.persist( owner );
		} );
		final var detached = scope.fromTransaction( session -> {
			session.enableFilter( "uniqueActive" );
			final var owner = switch ( loading ) {
				case "fetch" -> session.createSelectionQuery(
						"from UniqueOwner o left join fetch o.sql left join fetch o.filter", Owner.class ).getSingleResult();
				case "native" -> session.createNativeQuery( "select * from restricted_unique", Owner.class ).getSingleResult();
				default -> session.find( Owner.class, 1L );
			};
			assertThat( owner.sql ).isNull();
			assertThat( owner.filter ).isNull();
			owner.name = "updated";
			return owner;
		} );
		detached.name = "merged";
		scope.inTransaction( session -> {
			session.enableFilter( "uniqueActive" );
			session.merge( detached );
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select sql_code, filter_code from restricted_unique where id=1", Object[].class )
				.getSingleResult() ).containsExactly( "hidden-sql", "hidden-filter" ) );
	}
	@Entity(name = "UniqueSqlTarget")
	@Table(name = "restricted_unique_sql")
	@SQLRestriction("active = true")
	static class SqlTarget {
		@Id Long id;
		@Column(unique = true) String code;
		boolean active;
	}
	@Entity(name = "UniqueFilterTarget")
	@Table(name = "restricted_unique_filter")
	@FilterDef(name = "uniqueActive", applyToLoadByKey = true)
	@Filter(name = "uniqueActive", condition = "active = true")
	static class FilterTarget {
		@Id Long id;
		@Column(unique = true) String code;
		boolean active;
	}
	@Entity(name = "UniqueOwner")
	@Table(name = "restricted_unique")
	static class Owner {
		@Id Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "sql_code", referencedColumnName = "code")
		SqlTarget sql;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "filter_code", referencedColumnName = "code")
		FilterTarget filter;
	}
}
