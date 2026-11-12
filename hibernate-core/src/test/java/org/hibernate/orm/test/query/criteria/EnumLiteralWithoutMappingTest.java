package org.hibernate.orm.test.query.criteria;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21072")
@DomainModel(annotatedClasses = EnumLiteralWithoutMappingTest.LiteralEntity.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline"))
public class EnumLiteralWithoutMappingTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new LiteralEntity( 1L ) ) );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testConstantBodyProjection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Object.class );
			query.from( LiteralEntity.class );
			query.select( builder.literal( Status.CLOSED ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( Status.CLOSED );
		} );
	}

	@Test
	void testOrdinaryEnumProjection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Object.class );
			query.from( LiteralEntity.class );
			query.select( builder.literal( PlainStatus.CLOSED ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( PlainStatus.CLOSED );
		} );
	}

	@Test
	void testConstantBodyIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( LiteralEntity.class );
			query.select( root.get( "id" ) )
					.where( builder.isNotNull( builder.literal( Status.CLOSED ) ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( 1L );
		} );
	}

	@Test
	void testOrdinaryEnumIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( LiteralEntity.class );
			query.select( root.get( "id" ) )
					.where( builder.isNotNull( builder.literal( PlainStatus.CLOSED ) ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( 1L );
		} );
	}

	@Test
	void testConstantBodyCaseResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Status.class );
			final var root = query.from( LiteralEntity.class );
			query.select( builder.<Status>selectCase()
					.when( builder.equal( root.get( "id" ), 1L ), Status.CLOSED )
					.otherwise( Status.OPEN ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( Status.CLOSED );
		} );
	}

	@Test
	void testOrdinaryEnumCaseResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( PlainStatus.class );
			final var root = query.from( LiteralEntity.class );
			query.select( builder.<PlainStatus>selectCase()
					.when( builder.equal( root.get( "id" ), 1L ), PlainStatus.CLOSED )
					.otherwise( PlainStatus.OPEN ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( PlainStatus.CLOSED );
		} );
	}

	enum Status {
		OPEN {}, CLOSED {}
	}

	enum PlainStatus {
		OPEN, CLOSED
	}

	@Entity(name = "LiteralEntity")
	public static class LiteralEntity {
		@Id
		private Long id;

		public LiteralEntity() {
		}

		public LiteralEntity(Long id) {
			this.id = id;
		}
	}
}
