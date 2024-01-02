/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention( RUNTIME)
@Incubating
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
