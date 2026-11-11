package org.hibernate.type.internal;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.TemporalPreservationPolicy.Type;
import org.hibernate.cfg.MappingSettings;

/// Resolves factory defaults without changing descriptor selection or shared type state.
///
/// @author Steve Ebersole
public final class TemporalPolicySettings {
	private TemporalPolicySettings() {
	}

	private static final String[] KEYS = {
			MappingSettings.TEMPORAL_PRESERVATION_POLICY,
			MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME,
			MappingSettings.TEMPORAL_PRESERVATION_POLICY_ZONED_DATETIME,
			MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_TIME
	};

	public record Selection(Type policy, String source, boolean memberDeclaration) implements Serializable {
		public static final Selection DEFAULT = new Selection( Type.DOMAIN_VALUE, "domain default", false );
	}

	public static void validate(Map<String, Object> settings, boolean enabled) {
		for ( String key : KEYS ) {
			if ( settings.get( key ) != null ) {
				parse( settings.get( key ), key );
				if ( !enabled ) {
					throw new MappingException( "Setting '" + key + "' requires hibernate.type.temporal_preservation=true; remove the policy for compatibility mode" );
				}
			}
		}
	}

	public static Selection resolve(Map<String, Object> settings, Class<?> type) {
		final String specific = type == OffsetDateTime.class ? MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME
				: type == ZonedDateTime.class ? MappingSettings.TEMPORAL_PRESERVATION_POLICY_ZONED_DATETIME
				: type == OffsetTime.class ? MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_TIME : null;
		final String key = specific != null && settings.get( specific ) != null
				? specific
				: MappingSettings.TEMPORAL_PRESERVATION_POLICY;
		return settings.get( key ) == null
				? Selection.DEFAULT
				: new Selection( parse( settings.get( key ), key ), key, false );
	}

	private static Type parse(Object value, String source) {
		if ( value instanceof Type policy ) {
			return policy;
		}
		if ( value instanceof String string ) {
			try {
				return Type.valueOf( string.trim().toUpperCase( Locale.ROOT ) );
			}
			catch (IllegalArgumentException ignored) {
			}
		}
		throw new MappingException( "Invalid temporal preservation policy '" + value + "' for '" + source
				+ "'; expected domain_value or normalized" );
	}
}
