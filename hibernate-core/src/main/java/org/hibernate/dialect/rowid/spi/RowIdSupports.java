/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowid.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.rowid.internal.StandardRowIdSupport;

import static org.hibernate.SPI.Role.USE;

/// Supplies immutable stock row-id support profiles.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class RowIdSupports {
	private static final Map<FixedKey, RowIdSupport> FIXED = new ConcurrentHashMap<>();
	private static final Map<RequestedNameKey, RowIdSupport> REQUESTED_NAME = new ConcurrentHashMap<>();

	private RowIdSupports() {
	}

	/// Return the stable unsupported profile.
	public static RowIdSupport none() {
		return StandardRowIdSupport.none();
	}

	/// Create a fixed implicit pseudo-column profile.
	public static RowIdSupport fixed(String expression, int sqlTypeCode) {
		return FIXED.computeIfAbsent(
				new FixedKey( expression, sqlTypeCode ),
				key -> StandardRowIdSupport.fixed( key.expression(), key.sqlTypeCode() )
		);
	}

	/// Create a mapping-requested profile with an optional default and physical
	/// column-definition suffix.
	public static RowIdSupport requestedName(
			@Nullable String defaultExpression,
			int sqlTypeCode,
			@Nullable String columnDefinitionSuffix) {
		return REQUESTED_NAME.computeIfAbsent(
				new RequestedNameKey( defaultExpression, sqlTypeCode, columnDefinitionSuffix ),
				key -> StandardRowIdSupport.requestedName(
						key.defaultExpression(),
						key.sqlTypeCode(),
						key.columnDefinitionSuffix()
				)
		);
	}

	private record FixedKey(String expression, int sqlTypeCode) {
	}

	private record RequestedNameKey(
			@Nullable String defaultExpression,
			int sqlTypeCode,
			@Nullable String columnDefinitionSuffix) {
	}
}
