/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.usertype.UserType;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a custom {@linkplain UserType user type} implementation
 * to be used by default for all references to a particular class of
 * {@linkplain jakarta.persistence.Basic basic type}.
 * <p>
 * May be overridden for a specific entity field or property using
 * {@link Type @Type}.
 *
 * @see UserType
 * @see Type
 * @see CompositeTypeRegistration
 *
 * @author Gavin King
 *
 * @since 6.2
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE} )
@Retention( RUNTIME )
@Repeatable( TypeRegistrations.class )
public @interface TypeRegistration {
	/**
	 * The basic type described by the {@link #userType}.
	 */
	Class<?> basicClass();

	/**
	 * The {@link UserType}.
	 */
	Class<? extends UserType<?>> userType();
}
