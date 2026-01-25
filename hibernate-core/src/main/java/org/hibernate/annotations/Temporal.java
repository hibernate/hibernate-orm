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
 * <p>It is strongly recommended that every temporal entity declare
 * a {@linkplain jakarta.persistence.Version version} attribute.
 * The primary key of a table mapped by a temporal entity includes
 * the columns mapped by the identifier of the entity, along with
 * the version column, if there is one, or the {@linkplain #starting}
 * column if there is no version
 * <p>Associations targeting temporal entities do not have foreign
 * key constraints, and so <em>referential integrity is not enforced
 * by the database</em>. When working with temporal entities, it is
 * incredibly important to ensure that referential integrity is
 * maintained by the application and validated by offline processes.
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
	String starting() default "effective";
	/**
	 * The column name holding the ending timestamp of a revision.
	 * That is, the "effective to" timestamp.
	 */
	String ending() default "superseded";

	/**
	 * The fractional seconds precision for both temporal columns.
	 *
	 * @see jakarta.persistence.Column#secondPrecision()
	 */
	int secondPrecision() default -1;
}
