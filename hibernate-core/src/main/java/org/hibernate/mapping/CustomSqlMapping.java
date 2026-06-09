/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.jdbc.Expectation;

import static org.hibernate.internal.util.ReflectHelper.getDefaultSupplier;

/**
 * Custom SQL mutation details.
 *
 * @author Steve Ebersole
 */
public record CustomSqlMapping(
		String sql,
		boolean callable,
		Supplier<? extends Expectation> expectation) {

	public static CustomSqlMapping customSqlMapping(
			String sql,
			boolean callable,
			Class<? extends Expectation> expectationClass,
			boolean defaultToNone) {
		return new CustomSqlMapping(
				sql.trim(),
				callable,
				expectationSupplier( expectationClass, defaultToNone )
		);
	}

	private static Supplier<? extends Expectation> expectationSupplier(
			Class<? extends Expectation> expectationClass,
			boolean defaultToNone) {
		if ( expectationClass == Expectation.class ) {
			return defaultToNone ? Expectation.None::new : null;
		}
		return getDefaultSupplier( expectationClass );
	}
}
