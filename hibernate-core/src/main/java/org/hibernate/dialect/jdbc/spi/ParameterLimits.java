/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.jdbc.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable limits on JDBC parameters accepted by a database.
///
/// The two limits are independent: [#inExpressionCountLimit] applies to one
/// `IN` expression, while [#parameterCountLimit] applies to the complete JDBC
/// statement. A custom Dialect should return [#UNLIMITED] when neither limit
/// applies, use [#of(int)] when both limits are the same, or invoke the
/// canonical constructor when they differ. Nonpositive values are normalized
/// to `0`, the explicit unlimited value.
///
/// @param inExpressionCountLimit the maximum number of elements in one `IN`
/// expression, or a nonpositive value for no limit
/// @param parameterCountLimit the maximum number of JDBC parameters in the
/// complete statement, or a nonpositive value for no limit
///
/// @see Dialect#getParameterLimits()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public record ParameterLimits(int inExpressionCountLimit, int parameterCountLimit) {
	/// No limit on either `IN`-expression elements or statement parameters.
	public static final ParameterLimits UNLIMITED = new ParameterLimits( 0, 0 );

	public ParameterLimits {
		inExpressionCountLimit = normalize( inExpressionCountLimit );
		parameterCountLimit = normalize( parameterCountLimit );
	}

	/// Create a profile which applies the same limit to both dimensions.
	///
	/// @param limit the shared limit, or a nonpositive value for no limit
	/// @return [#UNLIMITED] for a nonpositive limit, or a profile containing the
	/// shared positive limit
	public static ParameterLimits of(int limit) {
		final int normalized = normalize( limit );
		return normalized == 0 ? UNLIMITED : new ParameterLimits( normalized, normalized );
	}

	private static int normalize(int limit) {
		return limit > 0 ? limit : 0;
	}
}
