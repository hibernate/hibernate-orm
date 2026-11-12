package org.hibernate.dialect.temporaltype.spi;

import java.util.Objects;
import java.util.function.UnaryOperator;

import org.hibernate.Incubating;

/// Immutable precision execution declaration for one resolved context. A supported
/// declaration certifies that prepared values are fixed points of the JDBC/storage
/// route and that its read representation is deterministic. It is not a declaration
/// that a retained schema matches the mapping.
///
/// Custom quantizers may model nondecimal grids; they must preserve null, be
/// idempotent, never mutate their input and retain required temporal components.
/// They must reject unrepresentable values rather than silently saturating them.
/// Functions must be immutable, thread-safe and independent of ambient timezone.
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
public final class TemporalPrecisionSupport {
	private final String signature;
	private final String unsupportedReason;
	private final UnaryOperator<Object> quantizer;
	private final UnaryOperator<Object> reconstruction;

	private TemporalPrecisionSupport(String signature, String unsupportedReason,
			UnaryOperator<Object> quantizer, UnaryOperator<Object> reconstruction) {
		this.signature = signature;
		this.unsupportedReason = unsupportedReason;
		this.quantizer = quantizer;
		this.reconstruction = reconstruction;
	}

	/// Declares a supported route. The stable signature must distinguish the grid,
	/// rule and read representation, including the contribution's semantic version.
	/// Do not use object identity or include credentials in this signature.
	///
	/// @param quantizer changes fractional precision only, without policy normalization
	/// @param reconstruction constructs the relational value returned by the read route;
	/// it may apply the route's declared representation, but must not quantize again
	public static TemporalPrecisionSupport supported(String signature,
			UnaryOperator<Object> quantizer, UnaryOperator<Object> reconstruction) {
		if ( signature == null || signature.isBlank() ) {
			throw new IllegalArgumentException( "Temporal precision support requires a stable semantic signature" );
		}
		return new TemporalPrecisionSupport( signature, null,
				Objects.requireNonNull( quantizer, "quantizer" ), Objects.requireNonNull( reconstruction, "reconstruction" ) );
	}

	/// Refuses this context without permitting stock fallback to weaken the refusal.
	public static TemporalPrecisionSupport unsupported(String reason) {
		if ( reason == null || reason.isBlank() ) {
			throw new IllegalArgumentException( "Unsupported temporal precision requires a reason" );
		}
		return new TemporalPrecisionSupport( null, reason, null, null );
	}

	public boolean isSupported() {
		return unsupportedReason == null;
	}

	public String getUnsupportedReason() {
		return unsupportedReason;
	}

	public String getSignature() {
		return signature;
	}

	/// Prepares the relational value for storage without changing its read representation.
	public Object quantize(Object value) {
		checkSupported();
		return value == null ? null : quantizer.apply( value );
	}

	/// Reconstructs an already prepared relational value as the actual read route does.
	public Object reconstruct(Object value) {
		checkSupported();
		return value == null ? null : reconstruction.apply( value );
	}

	private void checkSupported() {
		if ( !isSupported() ) {
			throw new IllegalStateException( unsupportedReason );
		}
	}
}
