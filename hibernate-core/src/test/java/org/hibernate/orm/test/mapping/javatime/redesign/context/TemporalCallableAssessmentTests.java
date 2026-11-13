package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.Types;
import java.util.Optional;
import java.util.function.UnaryOperator;

import jakarta.persistence.ParameterMode;

import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Boundary;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalPrecisionJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Assessment controls do not constitute live evidence of named or INOUT support.
///
/// @author Steve Ebersole
class TemporalCallableAssessmentTests {
	private final H2Dialect dialect = new H2Dialect( DatabaseVersion.make( 2, 5, 252 ) );
	private final BasicType<?> stock = new BasicTypeImpl<>( LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE );

	@Test
	void contextRejectsImpossibleRoutes() {
		assertThatThrownBy( () -> context( stock, ParameterMode.OUT, Boundary.INPUT, Access.INDEXED, Access.NONE, false ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> context( stock, ParameterMode.IN, Boundary.OUTPUT, Access.INDEXED, Access.INDEXED, false ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> context( stock, ParameterMode.OUT, Boundary.OUTPUT, Access.RESULT_SET, Access.NONE, false ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> context( stock, ParameterMode.REF_CURSOR, Boundary.OUTPUT, Access.INDEXED, Access.INDEXED, false ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void attributeSupportAndStockSubclassesDoNotCertifyCallable() {
		var attribute = new BasicTypeImpl<>( LocalDateTimeJavaType.INSTANCE, new AttributeOnlyJdbcType() );
		assertThat( dialect.getTemporalCallablePrecisionSupport( context( attribute, ParameterMode.IN,
				Boundary.INPUT, Access.INDEXED, Access.NONE, false ) ).isSupported() ).isFalse();
		var replacement = new BasicTypeImpl<>( LocalDateTimeJavaType.INSTANCE, new LocalDateTimeJdbcType() {} );
		assertThat( dialect.getTemporalCallablePrecisionSupport( context( replacement, ParameterMode.IN,
				Boundary.INPUT, Access.INDEXED, Access.NONE, false ) ).isSupported() ).isFalse();
	}

	@Test
	void directionsAndAccessAreIndependent() {
		var custom = TemporalCallableContractTests.type( "input3" );
		var input = context( custom, ParameterMode.INOUT, Boundary.INPUT, Access.INDEXED, Access.NONE, false );
		var output = context( custom, ParameterMode.INOUT, Boundary.OUTPUT, Access.NAMED, Access.INDEXED, false );
		assertThat( dialect.getTemporalCallablePrecisionSupport( input ).quantize( TemporalCallablePrecisionTests.INPUT ) )
				.isEqualTo( TemporalCallablePrecisionTests.INPUT.withNano( 123000000 ) );
		assertThat( dialect.getTemporalCallablePrecisionSupport( output ).quantize( TemporalCallablePrecisionTests.INPUT ) )
				.isEqualTo( TemporalCallablePrecisionTests.INPUT );
		assertThat( dialect.getTemporalCallablePrecisionSupport( context( stock, ParameterMode.INOUT,
				Boundary.INPUT, Access.INDEXED, Access.NONE, false ) ).isSupported() ).isFalse();
		assertThat( dialect.getTemporalCallablePrecisionSupport( context( stock, ParameterMode.IN,
				Boundary.INPUT, Access.NAMED, Access.NONE, false ) ).isSupported() ).isFalse();
	}

	private TemporalCallablePrecisionContext context(BasicType<?> type, ParameterMode mode, Boundary boundary,
			Access access, Access registration, boolean function) {
		return new TemporalCallablePrecisionContext( type, dialect, Types.TIMESTAMP, mode, boundary, access,
				registration, function, "assessment control", TemporalPreservationPolicy.Type.DOMAIN_VALUE, null );
	}

	static class AttributeOnlyJdbcType extends LocalDateTimeJdbcType implements TemporalPrecisionJdbcType {
		@Override
		public Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context) {
			return Optional.of( TemporalPrecisionSupport.supported( "attribute-only", UnaryOperator.identity(), UnaryOperator.identity() ) );
		}
	}
}
