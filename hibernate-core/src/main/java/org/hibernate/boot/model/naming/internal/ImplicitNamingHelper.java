/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.internal;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.relational.naming.spi.LogicalName;

/// Validates implicit results and bridges column consumers still accepting encoded names.
///
/// @author Steve Ebersole
public final class ImplicitNamingHelper {
	private ImplicitNamingHelper() {}

	public static String columnName(LogicalName name, String role) {
		if ( name == null || name.isExplicit() ) {
			throw new MappingException( "Implicit naming strategy must return a non-null implicit name for " + role );
		}
		return name.toString();
	}

	/// Share one lazy naming decision between materialization and alias registration.
	/// Explicit names do not evaluate this supplier.
	public static Supplier<String> once(Supplier<LogicalName> naming, String role) {
		return new Supplier<>() {
			private String result;
			@Override
			public String get() {
				if ( result == null ) {
					result = columnName( naming.get(), role );
				}
				return result;
			}
		};
	}
}
