/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/// Resolves the provider Dialect for the fixture's synthetic database name.
///
/// @author Steve Ebersole
/// @since 8.0
// tag::dialect-resolver[]
public final class ExampleDialectResolver implements DialectResolver {
	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		return "ExampleDB".equals( info.getDatabaseName() ) ? new ExampleDialect() : null;
	}
}
// end::dialect-resolver[]
