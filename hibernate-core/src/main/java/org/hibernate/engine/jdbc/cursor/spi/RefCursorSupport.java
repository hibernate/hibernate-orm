/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.SPI;
import org.hibernate.service.Service;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Perform JDBC REF_CURSOR registration and extraction as one coherent
/// strategy.
///
/// Implement both positional and named operations with matching registration
/// and extraction behavior. Convert JDBC failures through the
/// [RefCursorSupportCreationContext] received by the factory, do not retain a
/// statement, and prefer a stock factory from [RefCursorSupports] where its
/// behavior matches the driver.
///
/// @since 4.3
/// @author Steve Ebersole
@SPI(IMPLEMENT)
public interface RefCursorSupport extends Service {
	/// Register a positional parameter which returns a [ResultSet].
	void registerRefCursorParameter(CallableStatement statement, int position);

	/// Register a named parameter which returns a [ResultSet].
	void registerRefCursorParameter(CallableStatement statement, String name);

	/// Extract the result registered at the given position.
	ResultSet getResultSet(CallableStatement statement, int position);

	/// Extract the result registered under the given name.
	ResultSet getResultSet(CallableStatement statement, String name);
}
