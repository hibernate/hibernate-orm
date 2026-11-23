package org.hibernate.orm.test.where.annotations;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
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
@SessionFactory
class RestrictedToOneUniqueKeyTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void storedUniqueKeysArePreserved(boolean fetchJoin, SessionFactoryScope scope) {
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
			final var owner = fetchJoin ? session.createQuery(
					"from UniqueOwner o left join fetch o.sql left join fetch o.filter", Owner.class ).getSingleResult()
					: session.find( Owner.class, 1L );
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
