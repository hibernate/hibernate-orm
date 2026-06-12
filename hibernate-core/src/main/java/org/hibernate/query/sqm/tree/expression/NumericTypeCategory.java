/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Steve Ebersole
 */
public enum NumericTypeCategory {
	INTEGER,
	LONG,
	BIG_INTEGER,
	DOUBLE,
	FLOAT,
	BIG_DECIMAL;

	public <N extends Number> N parseLiteralValue(String value) {
		return switch ( this ) {
			case INTEGER ->
				//noinspection unchecked
					(N) Integer.valueOf( value );
			case LONG ->
				//noinspection unchecked
					(N) Long.valueOf( value );
			case BIG_INTEGER ->
				//noinspection unchecked
					(N) new BigInteger( value );
			case DOUBLE ->
				//noinspection unchecked
					(N) Double.valueOf( value );
			case FLOAT ->
				//noinspection unchecked
					(N) Float.valueOf( value );
			case BIG_DECIMAL ->
				//noinspection unchecked
					(N) new BigDecimal( value );
		};
	}
}
