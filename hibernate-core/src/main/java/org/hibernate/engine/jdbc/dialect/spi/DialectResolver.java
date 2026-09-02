/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.spi;

import org.hibernate.SPI;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Resolves a Dialect from database and driver information during bootstrap.
///
/// Expose an implementation through the Java [java.util.ServiceLoader]
/// facility or list its class name in [JdbcSettings#DIALECT_RESOLVERS].
/// Explicitly configured resolvers run before service-loaded resolvers, and
/// Hibernate's standard resolver runs last.
///
/// Return `null` when the supplied information is not recognized. Do not
/// retain the bootstrap-scoped resolution information or consume and close its
/// optional JDBC metadata handle.
///
/// @author Tomoto Shimizu Washio
/// @author Steve Ebersole
/// @see #resolveDialect(DialectResolutionInfo)
/// @see JdbcSettings#DIALECT_RESOLVERS
@JavaServiceLoadable
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface DialectResolver extends Service {
	/// Supply the Dialect selected for the database and driver information.
	///
	/// @param info the non-null database and driver information
	/// @return the selected Dialect, or `null` when this resolver declines the
	/// request
	/// @see Dialect
	@SPI({ USE, IMPLEMENT, SUPPLY })
	Dialect resolveDialect(DialectResolutionInfo info);
}
