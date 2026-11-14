package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Specifies how the time zone information of a persistent property or field should be persisted.
 * This annotation may be used in conjunction with the {@link jakarta.persistence.Basic} annotation,
 * or in conjunction with the {@link jakarta.persistence.ElementCollection} annotation when the
 * element collection value is of basic type. If the {@code TimeZoneStorage} annotation is not used,
 * the {@link TimeZoneStorageType} has a default value determined by the dialect and by the
 * configuration property {@value org.hibernate.cfg.AvailableSettings#TIMEZONE_DEFAULT_STORAGE}.
 * <p>
 * For example:
 * <pre>
 * &#64;Entity
 * public class Person {
 *
 *     &#64;Column(name = "birth_timestamp")
 *     &#64;TimeZoneColumn(name = "birth_zone")
 *     &#64;TimeZoneStorage(COLUMN)
 *     public OffsetDateTime birthDate;
 *
 *     &#64;TimeZoneStorage(NATIVE)
 *     public OffsetDateTime registrationDate;
 *
 *     ...
 * }
 * </pre>
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @see TimeZoneStorageType
 * @see TimeZoneColumn
 *
 * @deprecated Since 8.0, use {@link TimeZoneRepresentation} to select storage and
 * {@link TimeZoneAdjustment} to request an explicit timezone adjustment. With
 * {@code hibernate.type.temporal_preservation=true}, legacy {@code COLUMN}
 * selects {@code TimeZoneRepresentation.Type.COLUMN}; {@code NORMALIZE_UTC}
 * contributes INLINE storage and UTC adjustment. Legacy {@code NATIVE} retains
 * its native-handling constraint. {@code NORMALIZE}, {@code AUTO}, and
 * {@code DEFAULT} must be replaced with explicit choices. Disabling direct JDBC
 * handling retains the established legacy behavior.
 *
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD })
@Deprecated(since = "8.1")
public @interface TimeZoneStorage {
	/**
	 * The storage strategy for the time zone information.
	 */
	TimeZoneStorageType value() default TimeZoneStorageType.AUTO;
}
