package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Selects which parts of an [java.time.OffsetTime],
/// [java.time.OffsetDateTime] or [java.time.ZonedDateTime] must be preserved when
/// [temporal preservation][org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION]
/// is enabled.
///
/// By default every part of the value must be preserved: the instant and the
/// offset, and for a `ZonedDateTime` the zone as well. Many databases cannot
/// store all of that in one column. This annotation lets a mapping state that
/// the instant alone is what matters, which most databases can store. For an
/// `OffsetTime`, which denotes no instant, that is its time of day as
/// expressed at UTC.
///
/// The policy for a mapped attribute is taken from the first of the following
/// which declares one: the field or property itself, the class which declares
/// it, each superclass of that class in turn, the package of that class, the
/// module of that class, the setting for the Java type
/// ([org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME]
/// or [org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY_ZONED_DATETIME]),
/// and the general setting
/// ([org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY]). With
/// none of these, the policy is [Type#VALUE].
///
/// The annotation selects a requirement. It does not change how the value is
/// stored, which is selected with [TimeZoneRepresentation], and it does not
/// make an unsupported mapping supported: a mapping which cannot meet the
/// selected requirement is rejected. It has no effect when temporal
/// preservation is disabled.
///
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION_POLICY
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@Retention(RUNTIME)
@Target({ FIELD, METHOD, TYPE, PACKAGE, MODULE })
@Repeatable(TemporalPreservationPolicies.class)
public @interface TemporalPreservationPolicy {
	/// What must be preserved.
	Type value();

	/// The Java types the declaration applies to, each of which must be
	/// [java.time.OffsetTime], [java.time.OffsetDateTime] or
	/// [java.time.ZonedDateTime]. Empty, which is the default, applies it
	/// to all three. Must be empty on a field or
	/// property, where the type is that of the attribute.
	///
	/// On a class, package or module, a declaration naming the type of an
	/// attribute takes precedence over one which names no types.
	Class<?>[] types() default {};

	/// What must be preserved of a value.
	enum Type {
		/// Every part of the value: the instant, the offset and, for a
		/// `ZonedDateTime`, the zone.
		VALUE,
		/// The instant only, or for an `OffsetTime` the time of day at UTC
		/// only. The value read back denotes the same instant, or the same
		/// time at UTC, but may have a different offset or zone than the
		/// value written.
		NORMALIZED
	}
}
