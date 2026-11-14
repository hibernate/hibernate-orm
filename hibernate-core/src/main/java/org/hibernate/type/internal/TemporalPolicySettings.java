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

/// Parses and resolves session-factory defaults for temporal preservation policy.
/// A type-specific setting for [OffsetDateTime], [ZonedDateTime], or [OffsetTime]
/// takes precedence over the general policy setting. In the absence of either,
/// [Selection#DEFAULT] requires preservation of the domain value.
///
/// This class handles settings only. Mapping annotation scopes are resolved during
/// bootstrap, and the selected policy's applicability to a Java/JDBC type pair is
/// validated separately. Resolution does not select descriptors, adjust values,
/// or mutate shared type state.
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

	/// A selected policy together with the provenance needed for validation and diagnostics.
	///
	/// @param policy the required preservation policy
	/// @param source the setting key, annotation scope, or description of the implicit default
	/// @param memberDeclaration whether the policy was explicitly declared on the mapped
	/// member; validators use this to distinguish an invalid explicit request from a
	/// broader default that does not apply to the mapping's type
	public record Selection(Type policy, String source, boolean memberDeclaration) implements Serializable {
		/// Implicit domain-value policy, with no explicit setting or member declaration.
		public static final Selection DEFAULT = new Selection( Type.DOMAIN_VALUE, "domain default", false );
	}

	/// Validates every supplied policy setting, including type-specific settings that
	/// might not be selected by any mapping. Values may be [Type] constants or
	/// case-insensitive enum names, with surrounding whitespace ignored.
	///
	/// @param settings the factory configuration; absent and null values are treated as unset
	/// @param enabled the effective temporal-preservation flag, after setting precedence
	/// has been resolved
	/// @throws MappingException if a value is invalid or any policy setting is supplied
	/// while temporal preservation is disabled
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

	/// Selects the type-specific setting, general setting, or implicit domain default,
	/// in that order. Type selectors match the exact Java class, not assignability.
	/// Other types have no type-specific key and therefore use the general/default policy;
	/// this does not by itself establish that the policy applies to that type.
	///
	/// Call [#validate] separately to enforce compatibility-mode restrictions and check
	/// unselected settings. This method parses only the setting it selects.
	///
	/// @param settings the factory configuration, normally already validated
	/// @param type the Java type at the preservation boundary; for an application
	/// converter, this is its relational Java type rather than the entity attribute type
	/// @return the selected policy and its source, never null
	/// @throws MappingException if the selected setting has an invalid value
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
