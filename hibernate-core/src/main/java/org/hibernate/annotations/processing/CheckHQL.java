/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.processing;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Indicates that a package or top-level type contains HQL or JPQL
 * queries encoded as static strings that should be validated at
 * compile time by the Metamodel Generator or Query Validator.
 * Errors in queries are reported by the Java compiler.
 * <p>
 * The Metamodel Generator or Query Validator must be enabled as an
 * annotation processor in the project build. Otherwise, if neither
 * is enabled, this annotation has no effect.
 * <p>
 * If only the Metamodel Generator is enabled, only arguments to the
 * following annotations are validated:
 * <ul>
 * <li>{@link jakarta.persistence.NamedQuery#query},
 * <li>{@link org.hibernate.annotations.NamedQuery#query}.
 * </ul>
 * <p>
 * Otherwise, if the Query validator is enabled, then, within the
 * scope annotated {@code @CheckHQL}, any static string argument to
 * any one of the following methods is interpreted as HQL/JPQL and
 * validated:
 * <ul>
 * <li>{@link jakarta.persistence.EntityManager#createQuery(String,Class)},
 * <li>{@link jakarta.persistence.EntityManager#createQuery(String)},
 * <li>{@link org.hibernate.Session#createSelectionQuery(String,Class)}, or
 * <li>{@link org.hibernate.Session#createMutationQuery(String)}
 * </ul>
 * <p>
 * The entity classes referred to by the queries must be annotated
 * with basic JPA metadata annotations like {@code @Entity},
 * {@code @ManyToOne}, {@code @Embeddable}, {@code @MappedSuperclass},
 * {@code @ElementCollection}, and {@code @Access}. Metadata specified
 * in XML mapping documents is ignored by the query validator.
 * <p>
 * Syntax errors, unknown entity names and unknown entity member names,
 * and typing errors all result in compile-time errors.
 *
 * @see jakarta.persistence.NamedQuery#query()
 * @see jakarta.persistence.EntityManager#createQuery(String,Class)
 * @see org.hibernate.Session#createSelectionQuery(String,Class)
 *
 * @implNote The static HQL type checker is not aware of metadata defined
 *           purely in XML, nor of JPA converters, and therefore sometimes
 *           reports false positives. That is, it rejects queries at compile
 *           time that would be accepted at runtime.
 *           <p>
 *           Therefore, by default, HQL specified in {@code NamedQuery}
 *           annotations is always validated for both syntax and semantics,
 *           but only illegal syntax is reported with severity
 *           {@link javax.tools.Diagnostic.Kind#ERROR}. Problems with the
 *           semantics of HQL named queries (typing problem) are reported to
 *           the Java compiler by the Metamodel Generator with severity
 *           {@link javax.tools.Diagnostic.Kind#WARNING}.
 *           <p>
 *           So, actually, the effect of {@code CheckHQL} is only to change
 *           the severity of reported problem.
 *
 * @author Gavin King
 * @since 6.3
 */
@Target({PACKAGE, TYPE})
@Retention(CLASS)
@Incubating
public @interface CheckHQL {
	/**
	 * A Hibernate {@linkplain Dialect dialect} to use.
	 *
	 * @see Dialect
	 */
	Class<? extends Dialect> dialect() default GenericDialect.class;
}
