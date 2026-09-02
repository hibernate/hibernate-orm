/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.internal.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.internal.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.internal.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.internal.NoOpUniqueDelegate;
import org.hibernate.dialect.unique.internal.SkipNullableUniqueDelegate;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Supported composition facade for stock unique-key strategies.
///
/// Retain the returned strategy for the lifetime of its owning Dialect. Do not
/// import the Hibernate-owned implementations behind these factories.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class UniqueDelegates {
	private UniqueDelegates() {
	}

	public static UniqueDelegate alterTable(Dialect dialect) {
		return new AlterTableUniqueDelegate( requireNonNull( dialect, "dialect" ) );
	}

	public static UniqueDelegate createTable(Dialect dialect) {
		return new CreateTableUniqueDelegate( requireNonNull( dialect, "dialect" ) );
	}

	public static UniqueDelegate nullableIndex(Dialect dialect) {
		return new AlterTableUniqueIndexDelegate( requireNonNull( dialect, "dialect" ) );
	}

	public static UniqueDelegate alwaysIndex(Dialect dialect) {
		return new AlterTableUniqueIndexDelegate( requireNonNull( dialect, "dialect" ), true );
	}

	public static UniqueDelegate skipNullable(Dialect dialect) {
		return new SkipNullableUniqueDelegate( requireNonNull( dialect, "dialect" ) );
	}

	public static UniqueDelegate none() {
		return NoOpUniqueDelegate.INSTANCE;
	}
}
