package org.hibernate.type;

import java.util.Locale;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/// Controls whether {@linkplain java.time Java Time} objects are bound and
/// extracted at the JDBC boundary using direct JDBC 4.2 access
/// ({@code setObject}/{@code getObject}) or through legacy {@code java.sql}
/// wrappers ({@code Timestamp}, {@code Time}, {@code Date}).
///
/// @see org.hibernate.cfg.MappingSettings#JAVA_TIME_USE_DIRECT_JDBC
///
/// @since 8.1
@Incubating(since = "8.1")
public enum JavaTimeJdbcMode {
	/// Always use direct JDBC access for supported Java Time types.
	/// The configured {@code hibernate.jdbc.time_zone} and timezone storage
	/// strategy are not applied when using direct JDBC.
	TRUE,

	/// Never use direct JDBC access; always use legacy {@code java.sql}
	/// wrappers which honor {@code hibernate.jdbc.time_zone} and the
	/// configured timezone storage strategy.
	FALSE,

	/// Use direct JDBC access when it is safe to do so. An error is raised
	/// during bootstrap if a mapping is found that uses a direct Java Time
	/// JDBC type while the configuration or per-attribute annotations require
	/// JDBC timezone or timezone-storage normalization that direct access
	/// cannot honor.
	///
	/// This is the default.
	AUTO;

	/// Whether direct JDBC types should be preferred during type resolution.
	/// Returns {@code true} for both {@link #TRUE} and {@link #AUTO}.
	public boolean isPreferDirectJdbc() {
		return this != FALSE;
	}

	public static JavaTimeJdbcMode interpret(@Nullable Object value) {
		if ( value == null ) {
			return AUTO;
		}
		if ( value instanceof JavaTimeJdbcMode mode ) {
			return mode;
		}
		if ( value instanceof Boolean bool ) {
			return bool ? TRUE : FALSE;
		}
		var s = value.toString().trim().toLowerCase( Locale.ROOT );
		return switch ( s ) {
			case "true" -> TRUE;
			case "false" -> FALSE;
			case "auto" -> AUTO;
			default -> throw new IllegalArgumentException(
					"Unrecognized value for '" + org.hibernate.cfg.MappingSettings.JAVA_TIME_USE_DIRECT_JDBC
							+ "': " + value + " (expected true, false, or auto)"
			);
		};
	}
}
