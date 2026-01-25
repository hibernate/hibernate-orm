/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Instant;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the annotated entity class is a temporal entity
 * or temporal collection. A temporal entity or collection keeps a
 * historical record of changes over time. Each row of the mapped
 * table represents a single revision of a single instance of the
 * entity, or an element of the collection whose existence is
 * bounded in time, with:
 * <ul>
 * <li>a {@linkplain #starting} timestamp representing the instant
 *     at which the revision became effective, and
 * <li>an {@linkplain #ending} timestamp representing the instant at
 *     which the revision was superseded by a newer revision.
 * </ul>
 * The revision which is currently effective has a null value for its
 * {@linkplain #ending} timestamp.
 * <p>Given the identifier of an instance of a temporal entity, along
 * with an instant, which may be represented by an instance if
 * {@link java.time.Instant}, the effective revision of the temporal
 * entity with the given identifier at the given instant is the
 * revision with:
 * <ul>
 * <li>{@linkplain #starting} timestamp is less than or equal to the
 *     given instant, and
 * <li>{@linkplain #ending} timestamp is null or greater than the given
 *     instant.
 * </ul>
 * <p>By default, a session or stateless session reads revisions of
 * temporal data which are currently effective. To read historical
 * revisions effective at a given {@linkplain Instant instant}, set
 * {@linkplain org.hibernate.engine.creation.CommonBuilder#instant
 * the temporal data instant} when creating the session or stateless
 * session.
 *
 * @see org.hibernate.engine.creation.CommonBuilder#instant(Instant)
 */
@Documented
@Target({PACKAGE, TYPE, FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Incubating
public @interface Temporal {
	/**
	 * The column name holding the starting timestamp of a revision.
	 * That is, the "effective from" timestamp.
	 */
	String starting() default "starting";
	/**
	 * The column name holding the ending timestamp of a revision.
	 * That is, the "effective to" timestamp.
	 */
	String ending() default "ending";
}
