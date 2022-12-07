/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.usertype.UserType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a custom {@link UserType} for the annotated attribute mapping.
 * <p>
 * This is usually mutually exclusive with the compositional approach of
 * {@link JavaType}, {@link JdbcType}, etc.
 *
 * @see UserType
 * @see TypeRegistration
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Type {
	/**
	 * The implementation class which implements {@link UserType}.
	 */
	Class<? extends UserType<?>> value();

	/**
	 * Parameters to be injected into the custom type after it is
	 * instantiated. The {@link UserType} implementation must implement
	 * {@link org.hibernate.usertype.ParameterizedType} to receive the
	 * parameters.
	 */
	Parameter[] parameters() default {};
}
