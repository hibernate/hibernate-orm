package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.internal.DecimalTemporalPrecision;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;

/// Precision declarations for the H2 custom descriptors exercised by these
/// fixtures. Temporal-parts declarations deliberately do not imply this contract.
///
/// @author Steve Ebersole
final class TemporalTestPrecision {
	private TemporalTestPrecision() {
	}

	static Optional<TemporalPrecisionSupport> decimal(TemporalPrecisionContext context, UnaryOperator<Object> reconstruction) {
		if ( !(context.dialect() instanceof H2Dialect) ) {
			return Optional.empty();
		}
		final var quantizer = new DecimalTemporalPrecision( context.precision(), 9,
				context.dialect().getTemporalValueSemantics().precisionAdjustment(), context.adjustment() == null );
		return Optional.of( TemporalPrecisionSupport.supported( "test-h2:" + quantizer.signature(), quantizer, reconstruction ) );
	}
}
