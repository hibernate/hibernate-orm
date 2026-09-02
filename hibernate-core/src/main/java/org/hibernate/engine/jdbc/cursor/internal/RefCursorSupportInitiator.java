/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.SQLException;
import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.JDBCException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/// Service initiator for the [RefCursorSupport] service.
///
/// @author Steve Ebersole
public class RefCursorSupportInitiator implements StandardServiceInitiator<RefCursorSupport> {
	/// Singleton access.
	public static final RefCursorSupportInitiator INSTANCE = new RefCursorSupportInitiator();

	@Override
	public RefCursorSupport initiateService(
			@Nonnull Map<String, Object> configurationValues,
			@Nonnull ServiceRegistryImplementor registry) {
		final JdbcServices jdbcServices = registry.requireService( JdbcServices.class );
		final RefCursorSupportFactory factory = jdbcServices.getDialect().getRefCursorSupportFactory();
		if ( factory == null ) {
			throw new IllegalStateException( "Dialect supplied a null RefCursorSupportFactory" );
		}

		final RefCursorSupport support = factory.createRefCursorSupport( new RefCursorSupportCreationContext() {
			@Override
			public boolean supportsStandardRefCursors() {
				return jdbcServices.getJdbcMetadata().supportsRefCursors();
			}

			@Override
			public JDBCException convert(SQLException exception, String message) {
				return jdbcServices.getSqlExceptionHelper().convert( exception, message );
			}
		} );
		if ( support == null ) {
			throw new IllegalStateException(
					"RefCursorSupportFactory [" + factory + "] returned null; a non-null RefCursorSupport is required"
			);
		}
		return support;
	}

	@Nonnull
	@Override
	public Class<RefCursorSupport> getServiceInitiated() {
		return RefCursorSupport.class;
	}
}
