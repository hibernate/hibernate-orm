/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;
import org.hibernate.dialect.identity.spi.IdentityValueRetrieval;

/// Example provider implementation of the identity-column SPI.
///
/// A real provider should override only the SQL and retrieval behavior its
/// database supports, retain a stable instance, and supply that instance from
/// its Dialect.
///
/// @since 8.0
/// @author Steve Ebersole
public final class ExampleIdentityColumnSupport extends IdentityColumnSupportBase {
	public static final ExampleIdentityColumnSupport INSTANCE = new ExampleIdentityColumnSupport();

	private ExampleIdentityColumnSupport() {
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int jdbcTypeCode) {
		return "generated always as identity";
	}

	@Override
	public String getIdentityInsertString() {
		return "default";
	}

	@Override
	public IdentityValueRetrieval getIdentityValueRetrieval() {
		return IdentityValueRetrieval.NAMED_GENERATED_KEYS;
	}
}
