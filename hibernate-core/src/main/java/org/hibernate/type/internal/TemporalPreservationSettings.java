package org.hibernate.type.internal;

import java.util.Map;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.SqlTypes;

import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/// Immutable interpretation of the preservation setting and its deprecated alias.
/// Explicit JDBC type preferences are resolved separately and override the base code.
///
/// @author Steve Ebersole
public record TemporalPreservationSettings(boolean enabled, String source) {
	public static TemporalPreservationSettings resolve(Map<String, Object> settings) {
		final String source = settings.get( MappingSettings.TEMPORAL_PRESERVATION ) != null
				? MappingSettings.TEMPORAL_PRESERVATION
				: settings.get( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC ) != null
						? MappingSettings.JAVA_TIME_USE_DIRECT_JDBC : null;
		final var result = new TemporalPreservationSettings(
				source == null ? MappingSettings.TEMPORAL_PRESERVATION_DEFAULT
						: getBoolean( source, settings, MappingSettings.TEMPORAL_PRESERVATION_DEFAULT ), source );
		TemporalPolicySettings.validate( settings, result.enabled() );
		return result;
	}

	public int baseInstantJdbcType() {
		return enabled ? SqlTypes.INSTANT : SqlTypes.TIMESTAMP_UTC;
	}
}
