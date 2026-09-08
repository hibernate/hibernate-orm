/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable temporal precision-adjustment and literal-offset semantics.
///
/// @param precisionAdjustment whether reduced precision rounds or truncates
/// @param offsetLiteralSupport whether temporal literals accept explicit offsets
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTemporalValueSemantics()
@SPI({ USE, SUPPLY })
public record TemporalValueSemantics(
		PrecisionAdjustment precisionAdjustment,
		OffsetLiteralSupport offsetLiteralSupport) {
	public static final TemporalValueSemantics STANDARD =
			new TemporalValueSemantics( PrecisionAdjustment.ROUND, OffsetLiteralSupport.UNSUPPORTED );
	public static final TemporalValueSemantics ROUND_MAX =
			new TemporalValueSemantics( PrecisionAdjustment.ROUND_MAX, OffsetLiteralSupport.UNSUPPORTED );
	public static final TemporalValueSemantics TRUNCATING =
			new TemporalValueSemantics( PrecisionAdjustment.TRUNCATE, OffsetLiteralSupport.UNSUPPORTED );
	public static final TemporalValueSemantics OFFSET_LITERALS =
			new TemporalValueSemantics( PrecisionAdjustment.ROUND, OffsetLiteralSupport.SUPPORTED );
	public static final TemporalValueSemantics TRUNCATING_WITH_OFFSET_LITERALS =
			new TemporalValueSemantics( PrecisionAdjustment.TRUNCATE, OffsetLiteralSupport.SUPPORTED );

	public TemporalValueSemantics {
		if ( precisionAdjustment == null || offsetLiteralSupport == null ) {
			throw new IllegalArgumentException( "Temporal value-semantic axes must not be null" );
		}
	}

	/// Whether reducing temporal precision rounds instead of truncating.
	public boolean roundsOnOverflow() {
		return precisionAdjustment == PrecisionAdjustment.ROUND || precisionAdjustment == PrecisionAdjustment.ROUND_MAX;
	}

	/// Whether reducing temporal precision rounds first to the maximum precision before rounding to the
	/// actual precision instead of truncating.
	public boolean roundsToMaxPrecisionFirst() {
		return precisionAdjustment == PrecisionAdjustment.ROUND_MAX;
	}

	/// Whether temporal literals may include an explicit offset.
	public boolean supportsLiteralOffset() {
		return offsetLiteralSupport == OffsetLiteralSupport.SUPPORTED;
	}

	public enum PrecisionAdjustment { ROUND, ROUND_MAX, TRUNCATE }
	public enum OffsetLiteralSupport { UNSUPPORTED, SUPPORTED }
}
