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

/// Selects where the offset of an [java.time.OffsetTime] or
/// [java.time.OffsetDateTime], or the zone of a [java.time.ZonedDateTime],
/// is stored when
/// [temporal preservation][org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION]
/// is enabled.
///
/// The representation of a mapped attribute is taken from the first of the
/// following which declares one: the field or property itself, the class
/// which declares it, each superclass of that class in turn, the package of
/// that class, and the module of that class. With none of these, it is
/// [Type#DEFAULT].
///
/// This annotation says where the offset or zone is stored and nothing else. Which
/// parts of the value must be preserved is selected with
/// [TemporalPreservationPolicy]. It has no effect when temporal preservation
/// is disabled, where [TimeZoneStorage] applies instead; when temporal
/// preservation is enabled, [TimeZoneStorage] is rejected.
///
/// @see TimeZoneColumn
/// @see TimeZoneAdjustment
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@Retention(RUNTIME)
@Target({ FIELD, METHOD, TYPE, PACKAGE, MODULE })
public @interface TimeZoneRepresentation {
	/// Where the offset or zone is stored.
	Type value();

	/// Where the offset or zone of a value is stored.
	enum Type {
		/// Whichever of [#INLINE] and [#COLUMN] the database needs:
		/// [#INLINE] where the single column preserves every part of the
		/// value which its [TemporalPreservationPolicy] requires, and
		/// [#COLUMN] otherwise. The columns of the mapping therefore depend
		/// on the database, and what is preserved does not.
		DEFAULT,
		/// The value is stored in a single column, and its offset and zone
		/// are kept only as far as the type of that column keeps them.
		INLINE,
		/// The instant is stored in one column, and what the value has
		/// beyond its instant in a second column, which may be customized
		/// with [TimeZoneColumn]. For an [java.time.OffsetDateTime] the
		/// second column holds the offset from UTC as a number of seconds.
		/// So it does for an [java.time.OffsetTime], whose first column
		/// holds its time of day at UTC.
		/// For a [java.time.ZonedDateTime] it holds the identifier of the
		/// zone, such as `Europe/Paris`. Every database keeps the whole
		/// value this way.
		COLUMN
	}
}
