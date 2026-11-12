package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.type.descriptor.jdbc.TemporalPrecisionJdbcType;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.annotations.TemporalPreservationPolicy.Type.NORMALIZED;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.*;

/// A contributed execution path may normalize offsets only when its normalized-time guarantee is established.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalNormalizedTimeTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void normalizedExecutionWrapsWithoutInventingADate(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Normalized.class ) ) {
			int id = 0;
			for ( String text : List.of( "00:30:00+02:00", "23:30:00-02:00", "12:00:00+00:00:30" ) ) {
				final var row = new Normalized(); row.id = ++id; row.time = OffsetTime.parse( text );
				scope.tx( session -> session.persist( row ) );
				scope.factory.getCache().evictAllRegions();
				scope.tx( session -> {
					final var value = session.find( Normalized.class, row.id ).time;
					assertThat( value ).isEqualTo( row.time.withOffsetSameInstant( ZoneOffset.UTC ) );
					assertThat( value.getOffset() ).isEqualTo( ZoneOffset.UTC );
				} );
			}
			final var carry = new Normalized(); carry.id = 10; carry.time = OffsetTime.parse( "01:59:59.999999600+02:00" );
			final var empty = new Normalized(); empty.id = 11;
			scope.tx( session -> { session.persist( carry ); session.persist( empty ); } );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> {
				// Preparation carries before binding, so H2 receives the exact midnight fixed point.
				assertThat( session.find( Normalized.class, 10 ).time ).isEqualTo( OffsetTime.parse( "00:00Z" ) );
				assertThat( session.find( Normalized.class, 11 ).time ).isNull();
			} );
		}
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, Strict.class ) ) {}
		} ).isInstanceOf( MappingException.class ).hasMessageContaining( "TIME" );
	}

	@Entity(name = "PolicyNormalizedTime")
	static class Normalized {
		@Id int id;
		@JdbcType(NormalizingJdbcType.class)
		@TemporalPreservationPolicy(NORMALIZED)
		@Column(secondPrecision = 6)
		OffsetTime time;
	}

	@Entity(name = "PolicyStrictTime")
	static class Strict {
		@Id int id;
		@JdbcType(NormalizingJdbcType.class) OffsetTime time;
	}

	/// Test-only execution with a deliberately weaker guarantee than domain-value preservation.
	///
	/// @author Steve Ebersole
	public static class NormalizingJdbcType extends OffsetTimeJdbcType implements TemporalJdbcType, TemporalPrecisionJdbcType {
		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			return TemporalTestPrecision.decimal( context, value -> ((OffsetTime) value).withOffsetSameInstant( ZoneOffset.UTC ) );
		}

		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return javaType.getJavaTypeClass() == OffsetTime.class
					? Optional.of( new TemporalPreservation( IRRELEVANT, UNSUPPORTED, IRRELEVANT, UNSUPPORTED, IRRELEVANT, SUPPORTED ) )
					: Optional.empty();
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
			final var delegate = super.getBinder( javaType );
			return new BasicBinder<>( javaType, this ) {
				private X normalized(X value, WrapperOptions options) {
					return javaType.wrap( javaType.unwrap( value, OffsetTime.class, options )
							.withOffsetSameInstant( ZoneOffset.UTC ), options );
				}
				@Override
				protected void doBind(PreparedStatement statement, X value, int index, WrapperOptions options) throws SQLException {
					delegate.bind( statement, normalized( value, options ), index, options );
				}
				@Override
				protected void doBind(CallableStatement statement, X value, String name, WrapperOptions options) throws SQLException {
					delegate.bind( statement, normalized( value, options ), name, options );
				}
			};
		}
	}
}
