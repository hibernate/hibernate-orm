package org.hibernate.orm.test.where.annotations;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21131")
@DomainModel(annotatedClasses = RestrictedToOneCircularQueryCountTest.Target.class)
@SessionFactory(useCollectingStatementObserver = true)
class RestrictedToOneCircularQueryCountTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void circularFetchReusesRestrictedTarget(boolean alreadyLoaded, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shared = new Target( 100L, null );
			session.persist( shared );
			for ( long id = 1; id <= 5; id++ ) {
				final var child = new Target( id + 10, shared );
				session.persist( child );
				session.persist( new Target( id, child ) );
			}
		} );
		scope.inTransaction( session -> {
			if ( alreadyLoaded ) {
				// Populate the unique-key entry used by the circular association.
				session.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Target.class )
						.loadByUniqueKey( "code", "target-100", session );
			}
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			final var roots = session.createSelectionQuery(
					"from CircularRestrictedTarget t join fetch t.next where t.id<=5 order by t.id", Target.class )
					.getResultList();
			assertThat( roots ).hasSize( 5 );
			for ( var root : roots ) {
				assertThat( root.next.next ).isSameAs( roots.get( 0 ).next.next );
				assertThat( root.next.next.id ).isEqualTo( 100L );
			}
			assertThat( observer.getSqlQueries() ).hasSize( alreadyLoaded ? 1 : 2 );
		} );
	}

	@Entity(name = "CircularRestrictedTarget")
	@Table(name = "circular_restricted_target")
	@SQLRestriction("active = true")
	static class Target {
		@Id Long id;
		@Column(unique = true) String code;
		boolean active = true;
		@ManyToOne
		@JoinColumn(name = "next_code", referencedColumnName = "code")
		Target next;

		Target() {
		}

		Target(long id, Target next) {
			this.id = id;
			this.code = "target-" + id;
			this.next = next;
		}
	}
}
