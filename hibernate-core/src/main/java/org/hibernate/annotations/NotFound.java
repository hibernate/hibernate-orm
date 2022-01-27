/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that an association maps to a foreign key column without a
 * foreign key constraint, and which thus potentially violates referential
 * integrity.
 * <p>
 * The {@link #action} specifies how Hibernate should handle the case of
 * an orphaned foreign key with no associated row in the referenced table.
 * <ul>
 *     <li>{@link NotFoundAction#EXCEPTION} specifies that this situation
 *         should be treated as an error, and
 *     <li>{@link NotFoundAction#IGNORE} specifies that this situation
 *         should be treated as if the foreign key were null.
 * </ul>
 * Note that this annotation has the side effect of making the association
 * non-lazy.
 * <p>
 * This annotation implies
 * {@link jakarta.persistence.ConstraintMode#NO_CONSTRAINT} for the purposes
 * of DDL generation. That is, a foreign key constraint will not be included
 * in the generated DDL.
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface NotFound {
	/**
	 * Specifies how to handle the case of an orphaned foreign key.
	 * <p>
	 * By default, an {@linkplain NotFoundAction#EXCEPTION exception is thrown}.
	 */
	NotFoundAction action() default NotFoundAction.EXCEPTION;
}
