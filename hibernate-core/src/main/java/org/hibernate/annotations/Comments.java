/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Remove;
import org.hibernate.binder.internal.CommentsBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.Table;

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
 * @deprecated Per {@linkplain Comment}, prefer {@linkplain Table#comment()}
 */
@TypeBinderType(binder = CommentsBinder.class)
@AttributeBinderType(binder = CommentsBinder.class)
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
@Deprecated(since="7")
@Remove
public @interface Comments {
	Comment[] value();
}
