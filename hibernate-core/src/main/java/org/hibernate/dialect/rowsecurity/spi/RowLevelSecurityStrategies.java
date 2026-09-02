/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.spi;

import java.sql.Connection;
import java.util.List;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Provides stock row-level-security strategies.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class RowLevelSecurityStrategies {
	private RowLevelSecurityStrategies() {
	}

	/// Return the stable strategy used by Dialects which do not support native
	/// row-level security.
	public static RowLevelSecurity none() {
		return None.INSTANCE;
	}

	private enum None implements RowLevelSecurity {
		INSTANCE;

		@Override
		public boolean supportsRowLevelSecurity() {
			return false;
		}

		@Override
		public boolean supportsTenantIdentifierSource(TenantIdentifierSource source) {
			return false;
		}

		@Override
		public List<RowLevelSecurityDdl> getTenantTableDdl(RowLevelSecurityDdlRequest request) {
			return List.of();
		}

		@Override
		public void setTenantIdentifier(Connection connection, String tenantIdentifier, boolean root) {
		}
	}
}
