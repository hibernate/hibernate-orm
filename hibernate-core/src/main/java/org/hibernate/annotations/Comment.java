/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Remove;
import org.hibernate.binder.internal.CommentBinder;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a comment that will be included in generated DDL.
 * <p>
 * By default, if {@link #on} is <em>not</em> specified:
 * <ul>
 * <li>when a field or property is annotated, the comment applies to the mapped column,
 * <li>when a collection is annotated, the comment applies to the collection table, and
 * <li>when an entity class is annotated, the comment applies to the primary table.
 * </ul>
 * <p>
 * But when {@link #on} is explicitly specified, the comment applies to the mapped table
 * or column with the specified name.
 * <p>
 * For example:
 * <pre>
 * &#64;Entity
 * &#64;Table(name = "book")
 * &#64;SecondaryTable(name = "edition")
 * &#64;Comment("The primary table for Book")
 * &#64;Comment(on = "edition",
 *          value = "The secondary table for Book")
 * class Book { ... }
 * </pre>
 *
 * @apiNote In principle, it's possible for a column of a secondary table to have the
 *          same name as a column of the primary table, or as a column of some other
 *          secondary table. Therefore, {@link #on} may be ambiguous.
 *
 * @author Yanming Zhou
 * @author Gavin King
 *
 * @deprecated Prefer {@link jakarta.persistence.Table#comment},
 *             {@link jakarta.persistence.Column#comment}, etc.
 */
@TypeBinderType(binder = CommentBinder.class)
@AttributeBinderType(binder = CommentBinder.class)
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
@Repeatable(Comments.class)
@Deprecated(since="7")
@Remove
public @interface Comment {
	/**
	 * The text of the comment.
	 */
	String value();

	/**
	 * The name of the table or column to add the comment to.
	 */
	String on() default "";
}
