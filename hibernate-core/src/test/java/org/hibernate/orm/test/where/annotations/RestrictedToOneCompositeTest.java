package org.hibernate.orm.test.where.annotations;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedToOneCompositeTest.SqlTarget.class,
		RestrictedToOneCompositeTest.FilterTarget.class, RestrictedToOneCompositeTest.Owner.class })
@SessionFactory(useCollectingStatementObserver = true)
class RestrictedToOneCompositeTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void compositeKeysAreNotLost(boolean joinFetch, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 2; id++ ) {
				final var sql = new SqlTarget();
				sql.id = new Key( "sql", id );
				sql.active = id == 1;
				final var filter = new FilterTarget();
				filter.id = new Key( "filter", id );
				filter.active = id == 1;
				session.persist( sql );
				session.persist( filter );
				final var owner = new Owner();
				owner.id = id;
				owner.sql = sql;
				owner.filter = filter;
				session.persist( owner );
			}
		} );
		final Owner detached = scope.fromTransaction( session -> {
			session.enableFilter( "compositeActive" );
			final var owners = session.createQuery( "from CompositeOwner o"
					+ ( joinFetch ? " left join fetch o.sql left join fetch o.filter" : "" )
					+ " order by o.id", Owner.class ).getResultList();
			assertThat( owners ).hasSize( 2 );
			assertThat( owners.get( 0 ).sql ).isNotNull();
			assertThat( owners.get( 0 ).filter ).isNotNull();
			final var hidden = owners.get( 1 );
			assertThat( hidden.sql ).isNull();
			assertThat( hidden.filter ).isNull();
			hidden.name = "updated";
			return hidden;
		} );
		detached.name = "merged";
		scope.inTransaction( session -> {
			session.enableFilter( "compositeActive" );
			session.merge( detached );
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select sql_part, sql_number, filter_part, filter_number from restricted_composite where id=2",
				Object[].class ).getSingleResult() ).containsExactly( "sql", 2L, "filter", 2L ) );
	}
	@Test
	void compositeIdNavigationOnlyJoinsForEnabledFilters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var target = new FilterTarget();
			target.id = new Key( "filter", 2L );
			session.persist( target );
			final var owner = new Owner();
			owner.id = 2L;
			owner.filter = target;
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			final var inspector = scope.getCollectingStatementObserver();
			for ( boolean enabled : new boolean[] { false, true, false } ) {
				if ( enabled ) {
					session.enableFilter( "compositeActive" );
				}
				else {
					session.disableFilter( "compositeActive" );
				}
				for ( String hql : new String[] {
						"select o.filter.id.number from CompositeOwner o where o.id = 2",
						"select o.id from CompositeOwner o where o.filter.id.number = 2" } ) {
					inspector.clear();
					assertThat( session.createQuery( hql, Long.class ).getResultList() )
							.containsExactly( enabled ? new Long[0] : new Long[] { 2L } );
					assertThat( inspector.getSqlQueries() ).hasSize( 1 );
					assertThat( inspector.getSqlQueries().get( 0 ).contains( "join restricted_composite_filter " ) )
							.isEqualTo( enabled );
				}
				assertThat( session.createQuery(
						"select o.id from CompositeOwner o where o.filter.id = :key", Long.class )
						.setParameter( "key", new Key( "filter", 2L ) ).getResultList() )
						.containsExactly( enabled ? new Long[0] : new Long[] { 2L } );
			}
		} );
	}

	@Embeddable
	static class Key implements Serializable {
		String part;
		@Column(name = "key_number")
		Long number;
		Key() {}
		Key(String part, Long number) { this.part = part; this.number = number; }
		public boolean equals(Object object) { return object instanceof Key key && Objects.equals( part, key.part ) && Objects.equals( number, key.number ); }
		public int hashCode() { return Objects.hash( part, number ); }
	}
	@Entity(name = "CompositeSqlTarget")
	@Table(name = "restricted_composite_sql")
	@SQLRestriction("active = true")
	static class SqlTarget {
		@EmbeddedId Key id;
		boolean active;
	}
	@Entity(name = "CompositeFilterTarget")
	@Table(name = "restricted_composite_filter")
	@FilterDef(name = "compositeActive", applyToLoadByKey = true)
	@Filter(name = "compositeActive", condition = "active = true")
	static class FilterTarget {
		@EmbeddedId Key id;
		boolean active;
	}
	@Entity(name = "CompositeOwner")
	@Table(name = "restricted_composite")
	static class Owner {
		@Id Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "sql_part", referencedColumnName = "part")
		@JoinColumn(name = "sql_number", referencedColumnName = "key_number")
		SqlTarget sql;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "filter_part", referencedColumnName = "part")
		@JoinColumn(name = "filter_number", referencedColumnName = "key_number")
		FilterTarget filter;
	}
}
