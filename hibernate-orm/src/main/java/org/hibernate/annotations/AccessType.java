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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Property Access type.  Prefer the standard {@link javax.persistence.Access} annotation; however,
 * {@code @Access} is limited to field/property access definitions.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated Use {@link AttributeAccessor} instead; renamed to avoid confusion with the JPA
 * {@link javax.persistence.AccessType} enum.
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@Deprecated
public @interface AccessType {
	/**
	 * The access strategy name.
	 */
	String value();
}
