package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import org.hibernate.Incubating;

/// Selects the information a temporal mapping must preserve across JDBC storage.
/// Ultimately, this is a means to [lower][Type#NORMALIZED] preservability guarantees.
///
/// Declarations use the member, declaring class hierarchy, exact package, module,
/// and finally factory defaults.
///
/// This does not select time-zone [storage][TimeZoneColumn] or [adjustment][TimeZoneAdjustment].
///
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY_ZONED_DATETIME
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY_OFFSET_TIME
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@Retention(RUNTIME)
@Target({ FIELD, METHOD, TYPE, PACKAGE, MODULE })
@Repeatable(TemporalPreservationPolicies.class)
public @interface TemporalPreservationPolicy {
	/// The required preservation contract.
	Type value();

	/// Exact OffsetDateTime, ZonedDateTime or OffsetTime types to target at a broader scope.
	/// Empty selects the scope default.
	/// Must be empty on a mapped member.
	Class<?>[] types() default {};

	/// Requirements independent of the chosen storage representation.
	enum Type {
		/// Retain the domain value, subject to precision and explicit adjustment/storage choices.
		DOMAIN_VALUE,
		/// Retain the instant for offset/zoned date-times, or UTC-equivalent time of day
		/// modulo 24 hours for OffsetTime. Does not require UTC output.
		NORMALIZED
	}
}
