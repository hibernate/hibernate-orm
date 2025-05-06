/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.id.IdentifierGenerator;

import jakarta.persistence.Column;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describe the identifier for an id-bag.
 *
 * @see CollectionIdJavaClass
 * @see CollectionIdJavaType
 * @see CollectionIdJdbcType
 * @see CollectionIdJdbcTypeCode
 * @see CollectionIdMutability
 * @see CollectionIdType
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface CollectionId {
	/**
	 * The column containing the collection id.
	 */
	Column column() default @Column;

	/**
	 * Implementation for generating values.
	 *
	 * @apiNote Mutually exclusive with {@link #generator()}
	 */
	Class<? extends IdentifierGenerator> generatorImplementation() default IdentifierGenerator.class;

	/**
	 * The generator name.
	 * <p>
	 * Can specify either a built-in strategy ({@code "sequence"}, for example)
	 * or a named JPA id generator.
	 *
	 * @apiNote Mutually exclusive with {@link #generatorImplementation()}
	 */
	String generator() default "";
}
