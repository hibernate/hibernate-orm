/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.generator.Generator;
import org.hibernate.id.Configurable;
import org.hibernate.id.GenericGeneratorGeneration;
import org.hibernate.id.IdentifierGenerator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.GenerationType;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a {@linkplain Generator generator} used for generating entity
 * identifiers, allowing the use of custom identifier generation strategies
 * going beyond those provided by the four standard JPA-defined
 * {@linkplain GenerationType generation types}.
 * <p>
 * When {@code @GenericGenerator} directly annotates a field, the
 * {@link jakarta.persistence.GeneratedValue @GeneratedValue} annotation is
 * not required:
 * <pre>
 * &#64;Id
 * &#64;GenericGenerator(type = CustomUuidGenerator.class)
 * private String uuid;
 * </pre>
 * <p>
 * On the other hand, when {@code @GenericGenerator} annotates a class or
 * package, {@code @GeneratedValue} must be applied to the generated id
 * field:
 * <pre>
 * &#64;Entity
 * &#64;GenericGenerator(type = CustomUuidGenerator.class)
 * class Record {
 *     &#64;Id
 *     &#64;GeneratedValue
 *     private String uuid;
 *     ...
 * }
 * </pre>
 *
 * @apiNote This annotation has been substantially reworked after a period of
 * being marked as deprecated, and its semantics have changed significantly.
 * Previously, it registered a named generator using legacy infrastructure.
 * It is now redefined as an {@link IdGeneratorType} and the deprecation has
 * been reversed. Nevertheless, use of {@code @GenericGenerator} is still
 * only recommended as a migrational bridge to the recommended approach of
 * {@linkplain IdGeneratorType declaring a custom annotation} for each kind
 * of generator.
 *
 * @see IdGeneratorType
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD, TYPE, PACKAGE})
@Retention(RUNTIME)
@Incubating
@IdGeneratorType(GenericGeneratorGeneration.class)
public @interface GenericGenerator {
	/**
	 * The type of identifier generator, a class implementing {@link Generator}
	 * or, more commonly, {@link IdentifierGenerator}.
	 */
	Class<? extends Generator> type();

	/**
	 * Parameters to be passed to {@link Configurable#configure} when the
	 * identifier generator is instantiated.
	 *
	 * @apiNote Use of {@link Parameter @Parameter} to configure a generator
	 * is verbose and completely lacks type safety&mdash;it's much better to
	 * use {@link IdGeneratorType} to define a custom annotation type carrying
	 * the configuration of the specific generator class.
	 */
	Parameter[] parameters() default {};
}
