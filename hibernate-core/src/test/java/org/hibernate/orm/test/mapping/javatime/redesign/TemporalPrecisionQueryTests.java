package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcType;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.internal.DecimalTemporalPrecision;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics.PrecisionAdjustment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalPrecisionJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Assignment occurrences prepare for their own target grids; predicates retain
/// the original boundary even when the same named parameter supplies both roles.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
@DomainModel(annotatedClasses = TemporalPrecisionQueryTests.Event.class)
@SessionFactory(useCollectingStatementInspector = true)
class TemporalPrecisionQueryTests {
	private static final LocalDateTime INPUT = LocalDateTime.parse( "2026-01-02T12:34:56.123456789" );

	@Test
	void sharedParameterKeepsPerOccurrencePrecision(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				final var row = new Event();
				row.id = 1;
				row.millis = INPUT.withNano( 0 );
				row.micros = row.millis;
				session.persist( row );
			} );
			RecordingTimestamp.values.clear();
			scope.inTransaction( session -> assertThat( session.createMutationQuery(
					"update PrecisionQueryEvent set millis=:stamp, micros=:stamp where millis < :stamp" )
					.setParameter( "stamp", INPUT ).executeUpdate() ).isEqualTo( 1 ) );
			assertThat( RecordingTimestamp.values ).containsExactlyInAnyOrder(
					INPUT.withNano( 123_000_000 ), INPUT.withNano( 123_457_000 ), INPUT );
			scope.inTransaction( session -> {
				final var row = session.find( Event.class, 1 );
				assertThat( row.millis ).isEqualTo( INPUT.withNano( 123_000_000 ) );
				assertThat( row.micros ).isEqualTo( INPUT.withNano( 123_457_000 ) );
				assertThat( session.createSelectionQuery( "from PrecisionQueryEvent where millis=:stamp", Event.class )
						.setParameter( "stamp", INPUT ).getResultList() ).isEmpty();
				assertThat( session.createSelectionQuery( "from PrecisionQueryEvent where millis < :stamp", Event.class )
						.setParameter( "stamp", INPUT ).getResultList() ).hasSize( 1 );
			} );
		}
		finally {
			scope.dropData();
		}
	}

	@Test
	void insertValuesUsesEachTargetGrid(SessionFactoryScope scope) {
		try {
			RecordingTimestamp.values.clear();
			scope.inTransaction( session -> assertThat( session.createMutationQuery(
					"insert into PrecisionQueryEvent (id,millis,micros) values (2,:stamp,:stamp)" )
					.setParameter( "stamp", INPUT ).executeUpdate() ).isEqualTo( 1 ) );
			assertThat( RecordingTimestamp.values ).containsExactlyInAnyOrder(
					INPUT.withNano( 123_000_000 ), INPUT.withNano( 123_457_000 ) );
		}
		finally {
			scope.dropData();
		}
	}

	@Test
	void literalAssignmentPreparesBeforeRendering(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				final var row = new Event(); row.id = 3;
				session.persist( row );
			} );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			scope.inTransaction( session -> {
				final var builder = session.getCriteriaBuilder();
				final var update = builder.createCriteriaUpdate( Event.class );
				final var root = update.from( Event.class );
				update.set( root.<LocalDateTime>get( "millis" ), builder.literal( INPUT ) );
				assertThat( session.createMutationQuery( update ).executeUpdate() ).isEqualTo( 1 );
			} );
			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "12:34:56.123" ).doesNotContain( "123456789" );
			scope.inTransaction( session -> assertThat( session.find( Event.class, 3 ).millis )
					.isEqualTo( INPUT.withNano( 123_000_000 ) ) );
		}
		finally {
			scope.dropData();
		}
	}

	@Entity(name = "PrecisionQueryEvent")
	static class Event {
		@Id int id;
		@JdbcType(RecordingTimestamp.class) @Column(secondPrecision = 3) LocalDateTime millis;
		@JdbcType(RecordingTimestamp.class) @Column(secondPrecision = 6) LocalDateTime micros;
	}

	/// Records the value reaching the actual JDBC descriptor, after preparation.
	///
	/// @author Steve Ebersole
	public static class RecordingTimestamp extends LocalDateTimeJdbcType implements TemporalJdbcType, TemporalPrecisionJdbcType {
		static final List<Object> values = new ArrayList<>();

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return Optional.of( dialect.getTemporalPreservation( javaType, LocalDateTimeJdbcType.INSTANCE ) );
		}

		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			final var quantizer = new DecimalTemporalPrecision( context.precision(), 9, PrecisionAdjustment.ROUND, true );
			return Optional.of( TemporalPrecisionSupport.supported( "recording-h2:" + quantizer.signature(), quantizer, UnaryOperator.identity() ) );
		}

		@Override
		public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
			final var binder = super.getBinder( javaType );
			return new ValueBinder<>() {
				@Override
				public void bind(PreparedStatement statement, T value, int index, WrapperOptions options) throws SQLException {
					values.add( value );
					binder.bind( statement, value, index, options );
				}

				@Override
				public void bind(CallableStatement statement, T value, String name, WrapperOptions options) throws SQLException {
					values.add( value );
					binder.bind( statement, value, name, options );
				}
			};
		}
	}
}
