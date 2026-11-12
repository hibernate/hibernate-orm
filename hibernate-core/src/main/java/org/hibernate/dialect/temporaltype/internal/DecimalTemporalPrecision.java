package org.hibernate.dialect.temporaltype.internal;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics.PrecisionAdjustment;
import org.hibernate.type.descriptor.DateTimeUtils;

/// Pure decimal-grid quantization. Selecting this algorithm is not evidence that
/// a particular driver/column supports it; the resolved contribution owns that
/// assertion. Quantization preserves the Java representation and never applies
/// an ambient timezone or a normalization policy.
///
/// @author Steve Ebersole
public final class DecimalTemporalPrecision implements UnaryOperator<Object> {
	private final int precision;
	private final int maximumPrecision;
	private final PrecisionAdjustment adjustment;
	private final boolean preserveResolvedOffset;

	public DecimalTemporalPrecision(int precision, int maximumPrecision,
			PrecisionAdjustment adjustment, boolean preserveResolvedOffset) {
		if ( precision < 0 || precision > 9 || maximumPrecision < precision || maximumPrecision > 9 ) {
			throw new IllegalArgumentException( "Invalid temporal precision " + precision + "/" + maximumPrecision );
		}
		this.precision = precision;
		this.maximumPrecision = maximumPrecision;
		this.adjustment = Objects.requireNonNull( adjustment );
		this.preserveResolvedOffset = preserveResolvedOffset;
	}

	@Override
	public Object apply(Object value) {
		if ( value == null || value instanceof LocalDate ) {
			return value;
		}
		if ( !(value instanceof Temporal temporal) ) {
			throw new TemporalValueException( "Decimal temporal precision does not support " + value.getClass().getName() );
		}
		try {
			final Temporal result = switch ( adjustment ) {
				case TRUNCATE -> DateTimeUtils.truncateToPrecision( temporal, precision );
				case ROUND -> DateTimeUtils.roundToSecondPrecision( temporal, precision );
				case ROUND_MAX -> DateTimeUtils.roundToSecondPrecision(
						DateTimeUtils.roundToSecondPrecision( temporal, maximumPrecision ), precision );
			};
			if ( preserveResolvedOffset && value instanceof ZonedDateTime original
					&& !original.getOffset().equals( ((ZonedDateTime) result).getOffset() ) ) {
				throw new TemporalValueException( "Precision carry crosses a zone transition and cannot retain resolved offset "
						+ original.getOffset() + " in " + original.getZone() );
			}
			return result;
		}
		catch (DateTimeException | ArithmeticException exception) {
			throw new TemporalValueException( "Temporal precision carry is outside the representable range", exception );
		}
	}

	/// Stable algorithm identity; mapping signatures additionally include type and representation.
	public String signature() {
		return "decimal-v1:" + precision + ':' + maximumPrecision + ':' + adjustment + ':' + preserveResolvedOffset;
	}
}
