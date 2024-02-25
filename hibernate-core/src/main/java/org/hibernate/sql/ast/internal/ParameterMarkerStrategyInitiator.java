/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.ast.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * @author Steve Ebersole
 */
public class ParameterMarkerStrategyInitiator implements StandardServiceInitiator<ParameterMarkerStrategy> {
	/**
	 * Singleton access
	 */
	public static final ParameterMarkerStrategyInitiator INSTANCE = new ParameterMarkerStrategyInitiator();

	@Override
	public ParameterMarkerStrategy initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final boolean useNativeMarkers = ConfigurationHelper.getBoolean(
				AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS,
				configurationValues
		);

		if ( useNativeMarkers ) {
			final Dialect dialect = registry.requireService( JdbcServices.class ).getDialect();
			final ParameterMarkerStrategy nativeParameterMarkerStrategy = dialect.getNativeParameterMarkerStrategy();
			// the Dialect may return `null`, indicating falling-through to the standard strategy
			if ( nativeParameterMarkerStrategy != null ) {
				return nativeParameterMarkerStrategy;
			}
		}

		return ParameterMarkerStrategyStandard.INSTANCE;
	}

	@Override
	public Class<ParameterMarkerStrategy> getServiceInitiated() {
		return ParameterMarkerStrategy.class;
	}
}
