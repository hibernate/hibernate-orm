/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * compile time by the Hibernate Query Validator. The Query Validator
 * must be enabled as an annotation processor in the project build.
 * Otherwise, this annotation has no effect.
 * <p>
 * Within a scope annotated {@code @CheckHQL}, any static string
 * argument to any one the methods:
 * <ul>
 * <li>{@link jakarta.persistence.EntityManager#createQuery(String,Class)},
 * <li>{@link jakarta.persistence.EntityManager#createQuery(String)},
 * <li>{@link org.hibernate.Session#createSelectionQuery(String,Class)}, or
 * <li>{@link org.hibernate.Session#createMutationQuery(String)}
 * </ul>
 * or to any one of the annotation members:
 * <ul>
 * <li>{@link jakarta.persistence.NamedQuery#query},
 * <li>{@link org.hibernate.annotations.NamedQuery#query}, or
 * <li>{@link HQL#value}
 * </ul>
 * <p>
 * is interpreted as HQL/JPQL and validated. Errors in the query are
 * reported by the Java compiler.
 * <p>
 * The entity classes referred to in the queries must be annotated
 * with basic JPA metadata annotations like {@code @Entity},
 * {@code @ManyToOne}, {@code @Embeddable}, {@code @MappedSuperclass},
 * {@code @ElementCollection}, and {@code @Access}. Metadata specified
 * in XML mapping documents is ignored by the query validator.
 * <p>
 * Syntax errors, unknown entity names and unknown entity member names,
 * and typing errors all result in compile-time errors.
 *
 * @see HQL#value()
 * @see jakarta.persistence.NamedQuery#query()
 * @see jakarta.persistence.EntityManager#createQuery(String,Class)
 * @see org.hibernate.Session#createSelectionQuery(String,Class)
 *
 * @author Gavin King
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
