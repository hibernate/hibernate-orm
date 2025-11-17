/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.generator.Generator;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a named identifier generator, usually an instance of the interface
 * {@link org.hibernate.id.IdentifierGenerator}. This allows the use of custom
 * identifier generation strategies beyond those provided by the four basic
 * JPA-defined {@linkplain jakarta.persistence.GenerationType generation types}.
 * <p>
 * A named generator may be associated with an entity class by:
 * <ul>
 * <li>defining a named generator using this annotation, specifying an
 *     implementation of {@code IdentifierGenerator} using {@link #type},
 *     then
 * <li>annotating the identifier property of the entity with the JPA-defined
 *     {@link jakarta.persistence.GeneratedValue @GeneratedValue} annotation,
 *     and
 * <li>using {@link jakarta.persistence.GeneratedValue#generator() generator}
 *     to specify the {@link #name()} of the generator defined using this
 *     annotation.
 * </ul>
 * <p>
 * If neither {@link #type} not {@link #strategy} is specified, Hibernate asks
 * {@linkplain org.hibernate.dialect.Dialect#getNativeIdentifierGeneratorStrategy
 * the dialect} to decide an appropriate strategy. This is equivalent to using
 * {@link jakarta.persistence.GenerationType#AUTO AUTO} in JPA.
 * <p>
 * For example, if we define a generator using:
 * <pre>
 * &#64;GenericGenerator(name = "custom-generator",
 *                   type = org.hibernate.eg.CustomStringGenerator.class)
 * }</pre>
 * <p>
 * Then we may make use of it by annotating an identifier field as follows:
 * <pre>
 * &#64;Id &#64;GeneratedValue(generator = "custom-generator")
 * private String id;
 * </pre>
 * <p>
 * The disadvantage of this approach is the use of stringly-typed names. An
 * alternative, completely typesafe, way to declare a generator and associate
 * it with an entity is provided by the {@link IdGeneratorType @IdGeneratorType}
 * meta-annotation.
 *
 * @see jakarta.persistence.GeneratedValue
 *
 * @deprecated Use the new approach based on {@link IdGeneratorType}.
 *
 * @author Emmanuel Bernard
 */
@Target({PACKAGE, TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(GenericGenerators.class)
@Deprecated(since = "6.5", forRemoval = true)
public @interface GenericGenerator {
	/**
	 * The name of the identifier generator. This is the name that may be specified by
	 * the {@link jakarta.persistence.GeneratedValue#generator() generator} member of
	 * the {@code @GeneratedValue} annotation.
	 *
	 * @see jakarta.persistence.GeneratedValue#generator
	 */
	String name();
	/**
	 * The type of identifier generator, a class implementing {@link Generator}
	 * or, more commonly, {@link org.hibernate.id.IdentifierGenerator}.
	 *
	 * @since 6.2
	 */
	Class<? extends Generator> type() default Generator.class;
	/**
	 * The type of identifier generator, the name of either:
	 * <ul>
	 * <li>a built-in Hibernate id generator, or
	 * <li>a custom class implementing {@link Generator}, or, more commonly,
	 *     {@link org.hibernate.id.IdentifierGenerator}.
	 * </ul>
	 *
	 * @deprecated use {@link #type()} for typesafety
	 */
	@Deprecated(since="6.2", forRemoval = true)
	String strategy() default "native";
	/**
	 * Parameters to be passed to {@link org.hibernate.id.IdentifierGenerator#configure}
	 * when the identifier generator is instantiated.
	 */
	Parameter[] parameters() default {};
}
