/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

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
		switch ( this ) {
			case INTEGER: {
				//noinspection unchecked
				return (N) Integer.valueOf( value );
			}
			case LONG: {
				//noinspection unchecked
				return (N) Long.valueOf( value );
			}
			case BIG_INTEGER: {
				//noinspection unchecked
				return (N) new BigInteger( value );
			}
			case DOUBLE: {
				//noinspection unchecked
				return (N) Double.valueOf( value );
			}
			case FLOAT: {
				//noinspection unchecked
				return (N) Float.valueOf( value );
			}
			case BIG_DECIMAL: {
				//noinspection unchecked
				return (N) new BigDecimal( value );
			}
			default: {
				throw new IllegalStateException(
						String.format(
								Locale.ROOT,
								"Unable to parse numeric literal value `%s` - %s",
								value,
								name()
						)
				);
			}
		}
	}
}
