/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lob.spi;

import java.sql.DatabaseMetaData;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;

/// Provider base which forwards the complete LOB contract to one stable
/// delegate. Override only the provider-specific differences.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public abstract class DelegatingLobSupport implements LobSupport {
	private final LobSupport delegate;

	/// Create a selectively overriding strategy around a non-null delegate.
	@SPI(IMPLEMENT)
	protected DelegatingLobSupport(LobSupport delegate) {
		this.delegate = requireNonNull( delegate );
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(@Nullable DatabaseMetaData databaseMetaData) {
		return delegate.supportsJdbcConnectionLobCreation( databaseMetaData );
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return delegate.useInputStreamToInsertBlob();
	}

	@Override
	public boolean useConnectionToCreateLob() {
		return delegate.useConnectionToCreateLob();
	}

	@Override
	public boolean supportsMaterializedLobAccess() {
		return delegate.supportsMaterializedLobAccess();
	}

	@Override
	public boolean useMaterializedLobWhenCapacityExceeded() {
		return delegate.useMaterializedLobWhenCapacityExceeded();
	}

	@Override
	public boolean forceLobAsLastValue() {
		return delegate.forceLobAsLastValue();
	}

	@Override
	public boolean isLobType(int sqlTypeCode) {
		return delegate.isLobType( sqlTypeCode );
	}

	@Override
	public @Nullable String getValueLobFragmentForExtraCreateTableInfo(String columnName) {
		return delegate.getValueLobFragmentForExtraCreateTableInfo( columnName );
	}
}
