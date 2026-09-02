/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.SPI;
import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;
import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// External-provider example of current temporal expressions and callable
/// database timestamp retrieval.
///
/// @author Steve Ebersole
public enum ExampleCurrentTemporalSupport implements CurrentTemporalSupport {
	INSTANCE;

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return "fixture_current_date()";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "fixture_current_time()";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "fixture_current_timestamp()";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.callable( "{?=call fixture_current_timestamp()}" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean isCurrentTimestampStable() {
		return true;
	}
}
