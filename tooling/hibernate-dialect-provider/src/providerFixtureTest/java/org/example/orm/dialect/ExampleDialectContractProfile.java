/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.testing.spi.DialectContractProfile;

/// Contract-test profile published by the example external Dialect provider.
///
/// @author Steve Ebersole
// tag::profile[]
public final class ExampleDialectContractProfile implements DialectContractProfile {
	@Override
	public String name() {
		return "Example Dialect";
	}

	@Override
	public Dialect createDialect() {
		return new ExampleDialect();
	}

	@Override
	public DatabaseVersion expectedDatabaseVersion() {
		return DatabaseVersion.make( 1 );
	}
}
// end::profile[]
