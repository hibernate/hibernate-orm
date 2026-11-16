package org.hibernate.type.internal;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.annotations.TemporalPreservationPolicy.Type;

import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION_POLICY;
import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME;
import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION_POLICY_ZONED_DATETIME;

/// Resolves the temporal preservation policy selected by configuration
/// settings, for mappings which do not select one by annotation.
///
/// @see org.hibernate.annotations.TemporalPreservationPolicy
///
/// @since 8.1
/// @author Steve Ebersole
@Internal
public final class TemporalPolicySettings {
	private static final String[] KEYS = {
			TEMPORAL_PRESERVATION_POLICY,
			TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME,
			TEMPORAL_PRESERVATION_POLICY_ZONED_DATETIME
	};

	private TemporalPolicySettings() {
	}

	/// A selected policy and where it was selected.
	///
	/// @param policy the policy
	/// @param source the setting name or the annotated element, for diagnostics
	public record Selection(Type policy, String source) {
		/// The policy when nothing selects one.
		public static final Selection DEFAULT = new Selection( Type.VALUE, "the default" );
	}

	/// Whether the Java class is one a policy applies to.
	public static boolean appliesTo(Class<?> javaClass) {
		return javaClass == OffsetDateTime.class || javaClass == ZonedDateTime.class;
	}

	/// Check every policy setting which is given, whether or not any mapping
	/// ends up using it.
	public static void validate(Map<?, ?> settings) {
		for ( var key : KEYS ) {
			if ( settings.get( key ) != null ) {
				parse( settings.get( key ), key );
			}
		}
	}

	/// The policy selected by settings for the Java class: its own setting,
	/// then the general setting, then the default.
	public static Selection resolve(Map<?, ?> settings, Class<?> javaClass) {
		final String specific;
		if ( javaClass == OffsetDateTime.class ) {
			specific = TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME;
		}
		else if ( javaClass == ZonedDateTime.class ) {
			specific = TEMPORAL_PRESERVATION_POLICY_ZONED_DATETIME;
		}
		else {
			return Selection.DEFAULT;
		}
		final String key = settings.get( specific ) != null ? specific : TEMPORAL_PRESERVATION_POLICY;
		final Object value = settings.get( key );
		return value == null
				? Selection.DEFAULT
				: new Selection( parse( value, key ), "setting '" + key + "'" );
	}

	private static Type parse(Object value, String key) {
		if ( value instanceof Type policy ) {
			return policy;
		}
		if ( value instanceof String string ) {
			try {
				return Type.valueOf( string.trim().toUpperCase( Locale.ROOT ) );
			}
			catch (IllegalArgumentException ignored) {
				// fall through
			}
		}
		throw new MappingException(
				"Invalid value '" + value + "' for setting '" + key + "'; expected 'value' or 'normalized'"
		);
	}
}
