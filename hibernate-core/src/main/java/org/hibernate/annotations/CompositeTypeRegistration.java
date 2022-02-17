/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.usertype.CompositeUserType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers a custom composite user type implementation to be used
 * for all references to a particular {@link jakarta.persistence.Embeddable}.
 * <p/>
 * May be overridden for a specific embedded using {@link org.hibernate.annotations.CompositeType}
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE} )
@Retention( RUNTIME )
@Repeatable( CompositeTypeRegistrations.class )
public @interface CompositeTypeRegistration {
	Class<?> embeddableClass();
	Class<? extends CompositeUserType<?>> userType();
}
