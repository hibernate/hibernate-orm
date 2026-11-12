package org.hibernate.orm.test.mapping.javatime.redesign;

import java.util.function.Predicate;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/// Isolates a metadata assertion from an unproved provider execution prerequisite.
/// The caller must list its exact fixture and descriptor/DDL routes. Every other
/// context delegates to the real Dialect. The synthetic plan throws if executed:
/// it cannot be used as cache, quantization or JDBC evidence.
///
/// @author Steve Ebersole
public final class BootstrapOnlyTemporalPrecision {
	private BootstrapOnlyTemporalPrecision() {
	}

	public static Dialect isolate(Dialect dialect, Class<?> fixture, Predicate<TemporalPrecisionContext> route) {
		final var isolated = spy( dialect );
		doAnswer( invocation -> {
			final TemporalPrecisionContext context = invocation.getArgument( 0 );
			if ( context.role().startsWith( fixture.getName() + "." ) && route.test( context )
					&& (context.operation() == TemporalPrecisionContext.Operation.ATTRIBUTE
					|| context.operation() == TemporalPrecisionContext.Operation.VERSION) ) {
				System.out.println( "BOOTSTRAP_ONLY_PRECISION " + context );
				return TemporalPrecisionSupport.supported( "bootstrap-only:" + fixture.getName(),
						BootstrapOnlyTemporalPrecision::executionForbidden, BootstrapOnlyTemporalPrecision::executionForbidden );
			}
			return invocation.callRealMethod();
		} ).when( isolated ).getTemporalPrecisionSupport( any() );
		return isolated;
	}

	private static Object executionForbidden(Object value) {
		throw new AssertionError( "A bootstrap-only precision contribution must never execute" );
	}
}
