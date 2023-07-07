/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations.processing;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Identifies a method of an abstract class or interface as defining
 * the signature of a finder method, and being generated automatically
 * by the Hibernate Metamodel Generator.
 * <p>
 * For example:
 * <pre>
 * &#064;Find
 * Book getBookForIsbn(String isbn);
 *
 * &#064;Find
 * List&lt;Book&gt; getBooksWithTitle(String title);
 * </pre>
 * <p>
 * The return type of an annotated method must be an entity type {@code E},
 * or one of the following types:
 * <ul>
 * <li>{@link java.util.List java.util.List&lt;E&gt;},
 * <li>{@link org.hibernate.query.Query org.hibernate.query.Query&lt;E&gt;},
 * <li>{@link org.hibernate.query.SelectionQuery org.hibernate.query.SelectionQuery&lt;E&gt;},
 * <li>{@link jakarta.persistence.Query jakarta.persistence.Query&lt;E&gt;}, or
 * <li>{@link jakarta.persistence.TypedQuery jakarta.persistence.TypedQuery&lt;E&gt;}.
 * </ul>
 * <p>
 * The names and types of the parameters of a finder method must match
 * exactly with the names and types of persistent fields of the entity
 * type returned by the finder method.
 * <ul>
 * <li>If there is one parameter, and it matches the {@code @Id} or
 *     {@code @EmbeddedId} field of the entity, the finder method uses
 *     {@link jakarta.persistence.EntityManager#find(Class, Object)}
 *     to retrieve the entity.
 * <li>If the parameters match exactly with the {@code @NaturalId}
 *     fieldd of the entity, the finder method uses
 *     {@link org.hibernate.Session#byNaturalId(Class)} to retrieve the
 *     entity.
 * <li>Otherwise, the finder method builds and executes a
 *     {@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria
 *     query}.
 * </ul>
 *
 * @author Gavin King
 * @since 6.3
 */
@Target(METHOD)
@Retention(CLASS)
@Incubating
public @interface Find {}
