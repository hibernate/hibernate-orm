/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that the associated temporal value should be stored with fractional seconds.
 * Only valid for values which contain seconds.
 *
 * @apiNote The presence or absence of this annotation implies different semantics for time
 * versus timestamp based values.  By default, time values are stored without fractional seconds
 * whereas timestamp values are stored with a precision based on the
 * {@linkplain org.hibernate.dialect.Dialect#getDefaultTimestampPrecision Dialect default}
 *
 * @see java.time.Instant
 * @see java.time.LocalDateTime
 * @see java.time.LocalTime
 * @see java.time.OffsetDateTime
 * @see java.time.OffsetTime
 * @see java.time.ZonedDateTime
 * @see java.sql.Time
 * @see java.sql.Timestamp
 * @see java.util.Calendar
 *
 * @since 6.5
 *
 * @deprecated Use {@link jakarta.persistence.Column#secondPrecision} which was introduced
 *             in JPA 3.2
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention( RUNTIME)
@Incubating
@Deprecated(since = "7.0", forRemoval = true)
public @interface FractionalSeconds {
	/**
	 * The fractional precision for the associated seconds.  Generally this will be one of<ul>
	 *     <li>3 (milliseconds)</li>
	 *     <li>6 (microseconds)</li>
	 *     <li>9 (nanoseconds)</li>
	 * </ul>
	 */
	int value();
}
