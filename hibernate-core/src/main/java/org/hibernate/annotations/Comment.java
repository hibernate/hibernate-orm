/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.binder.internal.CommentBinder;

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
 *
 * @author Yanming Zhou
 */
@TypeBinderType(binder = CommentBinder.class)
@AttributeBinderType(binder = CommentBinder.class)
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
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
