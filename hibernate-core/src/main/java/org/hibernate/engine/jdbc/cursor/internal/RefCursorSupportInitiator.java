/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Service initiator for the {@link RefCursorSupport} service
 *
 * @author Steve Ebersole
 */
public class RefCursorSupportInitiator implements StandardServiceInitiator<RefCursorSupport> {
	/**
	 * Singleton access
	 */
	public static final RefCursorSupportInitiator INSTANCE = new RefCursorSupportInitiator();

	@Override
	public RefCursorSupport initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final JdbcServices jdbcServices = registry.requireService( JdbcServices.class );
		final boolean supportsRefCursors = useRefCursorSupport( jdbcServices );
		return supportsRefCursors
				? new StandardRefCursorSupport( jdbcServices )
				: new FallbackRefCursorSupport (jdbcServices );
	}

	private boolean useRefCursorSupport(JdbcServices jdbcServices) {
		final Boolean dialectAnswer = jdbcServices.getDialect().supportsRefCursors();
		if ( dialectAnswer != null ) {
			return dialectAnswer;
		}
		return jdbcServices.getJdbcEnvironment().getExtractedDatabaseMetaData().supportsRefCursors();
	}

	@Override
	public Class<RefCursorSupport> getServiceInitiated() {
		return RefCursorSupport.class;
	}
}
