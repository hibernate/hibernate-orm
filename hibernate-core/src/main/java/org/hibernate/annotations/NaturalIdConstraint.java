/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the name of the unique constraint generated for
 * the {@link NaturalId} of an entity.
 * <p>
 * By default, Hibernate generates the name of the unique constraint
 * for a natural id using the configured implicit naming strategy.
 * This annotation allows explicitly specifying the constraint name.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @Entity
 * @NaturalIdConstraint(name = "person_ssn")
 * class Person {
 *
 *     @Id
 *     Long id;
 *
 *     @NaturalId
 *     String ssn;
 * }
 * }
 * </pre>
 *
 * @author Utsav Mehta
 * @see NaturalId
 */
@Target(TYPE)
@Retention(RUNTIME)
@Incubating
public @interface NaturalIdConstraint {

	/**
	 * The name of the unique constraint generated for the
	 * natural id of the entity.
	 */
	String name();
}
