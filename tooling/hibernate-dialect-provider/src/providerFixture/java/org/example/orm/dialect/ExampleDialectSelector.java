/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.Dialect;

/// Selects the provider Dialect for its explicit short name.
///
/// @author Steve Ebersole
/// @since 8.0
// tag::dialect-selector[]
public final class ExampleDialectSelector implements DialectSelector {
	@Override
	public Class<? extends Dialect> resolve(String name) {
		return "Example".equals( name ) ? ExampleDialect.class : null;
	}
}
// end::dialect-selector[]
