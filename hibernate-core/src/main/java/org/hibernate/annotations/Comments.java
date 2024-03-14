/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.Remove;
import org.hibernate.binder.internal.CommentsBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A list of {@link Comment}s.
 * <p>
 * If there are multiple {@link Comment}s on a class or attribute,
 * they must have distinct {@link Comment#on() on} members.
 *
 * @author Gavin King
 *
 * @remove JPA 3.2 adds a comment attribute to {@linkplain jakarta.persistence.Table}
 */
@TypeBinderType(binder = CommentsBinder.class)
@AttributeBinderType(binder = CommentsBinder.class)
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
@Remove
public @interface Comments {
	Comment[] value();
}
