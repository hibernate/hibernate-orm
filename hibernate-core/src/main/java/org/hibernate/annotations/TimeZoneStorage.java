/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

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
 * @since 6.0
 */
@Incubating
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD, METHOD })
public @interface TimeZoneStorage {
	/**
	 * The storage strategy for the time zone information.
	 */
	TimeZoneStorageType value() default TimeZoneStorageType.AUTO;
}
