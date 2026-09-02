/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Create the single REF_CURSOR JDBC access strategy used by a service
/// registry.
///
/// Inspect the creation context once, return a non-null service, and do not
/// retain the context in the factory. Supply one stable factory from the
/// Dialect for its entire lifetime.
///
/// @see Dialect#getRefCursorSupportFactory()
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ IMPLEMENT, SUPPLY })
public interface RefCursorSupportFactory {
	/// Create the non-null REF_CURSOR support service without retaining the
	/// creation context in this factory.
	RefCursorSupport createRefCursorSupport(RefCursorSupportCreationContext context);
}
