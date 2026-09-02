/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SampleDialect;

/// Selector fixture with a canonical name and a legacy alias.
///
/// @author Steve Ebersole
public class DefaultDialectSelector implements DialectSelector {
	@Override
	public Class<? extends Dialect> resolve(String name) {
		return switch ( name ) {
			case "Sample", "SampleLegacy" -> SampleDialect.class;
			default -> null;
		};
	}
}
