package org.hibernate.orm.test.where.annotations;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedToOneTest.SqlTarget.class, RestrictedToOneTest.FilterTarget.class,
		RestrictedToOneRequiredTest.SqlRequired.class, RestrictedToOneRequiredTest.FilterRequired.class })
@SessionFactory
@ServiceRegistry(settings = @Setting(name = "hibernate.check_nullability", value = "true"))
class RestrictedToOneRequiredTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.getSessionFactory().getCache().evictAllRegions();
	}
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void filteredNullDoesNotViolateDatabaseNullability(boolean filter, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			if ( filter ) {
				final var target = new RestrictedToOneTest.FilterTarget();
				target.id = 2L;
				session.persist( target );
				final var owner = new FilterRequired();
				owner.id = 1L;
				owner.target = target;
				session.persist( owner );
			}
			else {
				final var target = new RestrictedToOneTest.SqlTarget();
				target.id = 2L;
				session.persist( target );
				final var owner = new SqlRequired();
				owner.id = 1L;
				owner.target = target;
				session.persist( owner );
			}
		} );
		final var detached = scope.fromTransaction( session -> {
			RestrictedToOneTest.enable( session );
			final var owner = filter ? session.find( FilterRequired.class, 1L ) : session.find( SqlRequired.class, 1L );
			assertThat( owner ).isNotNull();
			assertThat( owner.getTarget() ).isNull();
			owner.name = "updated";
			return owner;
		} );
		detached.name = "merged";
		scope.inTransaction( session -> {
			RestrictedToOneTest.enable( session );
			session.merge( detached );
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select target_id from " + ( filter ? "filter_required" : "sql_required" ), Long.class )
				.getSingleResult() ).isEqualTo( 2L ) );
	}
	@Entity(name = "SqlRequired")
	@Table(name = "sql_required")
	static class SqlRequired extends RestrictedToOneTest.Owner {
		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "target_id", nullable = false)
		RestrictedToOneTest.SqlTarget target;
		public RestrictedToOneTest.Target getTarget() { return target; }
		public void setTarget(RestrictedToOneTest.Target target) { this.target = (RestrictedToOneTest.SqlTarget) target; }
	}
	@Entity(name = "FilterRequired")
	@Table(name = "filter_required")
	static class FilterRequired extends RestrictedToOneTest.Owner {
		@ManyToOne(optional = false)
		@JoinColumn(name = "target_id", nullable = false)
		RestrictedToOneTest.FilterTarget target;
		public RestrictedToOneTest.Target getTarget() { return target; }
		public void setTarget(RestrictedToOneTest.Target target) { this.target = (RestrictedToOneTest.FilterTarget) target; }
	}
}
