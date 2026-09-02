/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.spi;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.testing.DialectTestKit;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Defines one stable, database-free Dialect test profile supplied by a
/// provider to [DialectTestKit].
///
/// Implementations must return a nonblank stable [#name], a fresh [Dialect]
/// from each [#createDialect] invocation, and a non-null expected version.
/// Settings must be safe during metadata boot without JDBC metadata, a live
/// connection, or schema execution. They must not select an alternate Dialect.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see DialectTestKit#contractTests(DialectContractProfile)
/// @see DialectTestKit#openContext(DialectContractProfile)
@Incubating
@SPI({ IMPLEMENT, SUPPLY })
public interface DialectContractProfile {
	/// A stable, human-readable profile name used for dynamic test reporting.
	String name();

	/// Create a fresh Dialect instance for a test-kit context.
	Dialect createDialect();

	/// The database version the created Dialect is expected to report.
	DatabaseVersion expectedDatabaseVersion();

	/// Additional database-free bootstrap settings.
	default Map<String, Object> settings() {
		return Map.of();
	}

	/// Determine whether the given optional contract applies.
	default ContractApplicability applicability(DialectContract contract) {
		return ContractApplicability.applicable();
	}
}
