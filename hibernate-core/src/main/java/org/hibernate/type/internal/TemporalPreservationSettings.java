package org.hibernate.type.internal;

import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.internal.util.config.ConfigurationException;

import static org.hibernate.cfg.MappingSettings.JAVA_TIME_USE_DIRECT_JDBC;
import static org.hibernate.cfg.MappingSettings.JAVA_TIME_USE_DIRECT_JDBC_DEFAULT;
import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION;
import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION_DEFAULT;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/// Resolves the two independent settings which govern Java Time values at
/// the JDBC boundary.
///
/// [org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION] alone decides
/// whether mappings and values are validated and possibly rejected.
///
/// Direct JDBC access is preferred when either setting is enabled. Enabling
/// temporal preservation while explicitly disabling
/// [org.hibernate.cfg.MappingSettings#JAVA_TIME_USE_DIRECT_JDBC] is
/// contradictory and is rejected.
///
/// @param preservationEnabled whether temporal preservation is enabled
/// @param directJdbcPreferred whether Java Time values should be passed to
///        and requested from the JDBC driver directly where the Dialect
///        reports support
///
/// @since 8.1
/// @author Steve Ebersole
@Internal
public record TemporalPreservationSettings(boolean preservationEnabled, boolean directJdbcPreferred) {

	public static TemporalPreservationSettings resolve(Map<?, ?> settings) {
		final boolean preservationEnabled = getBoolean( TEMPORAL_PRESERVATION, settings, TEMPORAL_PRESERVATION_DEFAULT );
		final boolean directJdbcGiven = settings.get( JAVA_TIME_USE_DIRECT_JDBC ) != null;
		final boolean directJdbc = getBoolean( JAVA_TIME_USE_DIRECT_JDBC, settings, JAVA_TIME_USE_DIRECT_JDBC_DEFAULT );
		if ( preservationEnabled && directJdbcGiven && !directJdbc ) {
			throw new ConfigurationException(
					"Setting '" + JAVA_TIME_USE_DIRECT_JDBC + "' is explicitly disabled while '" + TEMPORAL_PRESERVATION
					+ "' is enabled; temporal preservation decides for itself where Java Time values are passed"
					+ " to the JDBC driver directly, so remove one of the two settings"
			);
		}
		return new TemporalPreservationSettings( preservationEnabled, preservationEnabled || directJdbc );
	}
}
