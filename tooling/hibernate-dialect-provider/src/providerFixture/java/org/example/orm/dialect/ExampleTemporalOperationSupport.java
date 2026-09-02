/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import jakarta.annotation.Nullable;
import jakarta.persistence.TemporalType;

import org.hibernate.SPI;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.query.common.TemporalUnit;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// External-provider example of nonstandard temporal-operation patterns.
///
/// @author Steve Ebersole
public enum ExampleTemporalOperationSupport implements TemporalOperationSupport {
	INSTANCE;

	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return "fixture_extract(?1,?2)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return "fixture_extract_" + unit;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateDurationField(TemporalUnit unit) {
		return "fixture_duration_" + unit;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(
			TemporalUnit unit,
			TemporalType temporalType,
			@Nullable IntervalType intervalType) {
		return "fixture_add(?1,?2,?3)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(
			TemporalUnit unit,
			TemporalType fromTemporalType,
			TemporalType toTemporalType) {
		return "fixture_diff(?1,?2,?3)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000;
	}
}
