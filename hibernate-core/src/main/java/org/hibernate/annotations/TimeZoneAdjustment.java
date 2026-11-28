package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.MODULE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Declares the time zone of every value of an [java.time.OffsetTime],
/// [java.time.OffsetDateTime] or [java.time.ZonedDateTime] attribute when
/// [temporal preservation][org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION]
/// is enabled.
///
/// The zone is a fixed part of the mapping and is not stored. The value is
/// stored in a single column, of which only the instant is required, and is
/// always read back in the declared zone. For an `OffsetDateTime`, that is the
/// offset which the declared zone has at the instant of the value. For an
/// `OffsetTime`, which has no date by which to determine the offset of a
/// region, the zone must be a fixed offset.
///
/// What happens to a value which is written in another zone or offset is
/// decided by the [TemporalPreservationPolicy] of the attribute:
///
/// - with [TemporalPreservationPolicy.Type#VALUE], it is rejected with a
///   [org.hibernate.TemporalPreservationException], since its own zone or
///   offset would be lost. This includes a value bound to a query parameter
///   which is compared with the attribute;
/// - with [TemporalPreservationPolicy.Type#NORMALIZED], it is converted to
///   the declared zone, keeping its instant.
///
/// The zone of an attribute is taken from the first of the following which
/// declares one: the field or property itself, the class which declares it,
/// each superclass of that class in turn, the package of that class, and the
/// module of that class. A declaration of [#NONE] cancels one which would
/// otherwise be inherited.
///
/// A declared zone cannot be combined with [TimeZoneRepresentation.Type#COLUMN],
/// which stores the zone or offset of each value. A declaration on a class,
/// package or module is not applied to an identifier or version, nor to an
/// attribute with an [jakarta.persistence.AttributeConverter] or a custom
/// type; a declaration on such a field or property itself is rejected.
///
/// This annotation has no effect when temporal preservation is disabled.
///
/// @see TemporalPreservationPolicy
/// @see TimeZoneRepresentation
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@Retention(RUNTIME)
@Target({ FIELD, METHOD, TYPE, PACKAGE, MODULE })
public @interface TimeZoneAdjustment {
	/// Cancels a declaration which would otherwise be inherited.
	String NONE = "<none>";

	/// The identifier of the zone, as accepted by [java.time.ZoneId#of(String)],
	/// for example `Europe/Paris`, `UTC` or `+02:00`, or [#NONE].
	String value();
}
