package org.hibernate.dialect.temporal.spi;

import java.time.Instant;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.hibernate.TemporalValueException;

/// Immutable provider assertion that canonical timestamps survive its complete
/// period storage route. Functions must be pure, idempotent and thread-safe.
/// The signature identifies encoding, grid, rounding and producer semantics.
///
/// @author Steve Ebersole
/// @since 8.1
public final class TemporalPeriodPrecisionSupport {
	private final String signature;
	private final String reason;
	private final UnaryOperator<Instant> canonicalizer;
	private final String currentExpression;

	private TemporalPeriodPrecisionSupport(String signature, String reason,
			UnaryOperator<Instant> canonicalizer, String currentExpression) {
		this.signature = signature;
		this.reason = reason;
		this.canonicalizer = canonicalizer;
		this.currentExpression = currentExpression;
	}

	public static TemporalPeriodPrecisionSupport supported(String signature,
			UnaryOperator<Instant> canonicalizer, String currentExpression) {
		if ( signature == null || signature.isBlank() ) {
			throw new IllegalArgumentException( "Period precision needs a semantic signature" );
		}
		return new TemporalPeriodPrecisionSupport( signature, null, Objects.requireNonNull( canonicalizer ), currentExpression );
	}

	public static TemporalPeriodPrecisionSupport unsupported(String reason) {
		return new TemporalPeriodPrecisionSupport( null, Objects.requireNonNull( reason ), null, null );
	}

	public boolean isSupported() { return canonicalizer != null; }
	public String getSignature() { return signature; }
	public String getReason() { return reason; }
	public String getCurrentExpression() { return currentExpression; }

	public Instant canonicalize(Instant value) {
		if ( value == null ) {
			return null;
		}
		try {
			return Objects.requireNonNull( canonicalizer.apply( value ), "canonical period timestamp" );
		}
		catch (RuntimeException failure) {
			throw new TemporalValueException( "Cannot prepare changeset timestamp using period precision " + signature, failure );
		}
	}
}
