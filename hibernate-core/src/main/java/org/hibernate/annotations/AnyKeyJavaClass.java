/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the Java class to use for the foreign key handling
 * related to an {@link Any} mapping.
 * <p>
 * The specified class is resolved to a {@link BasicJavaType}
 * via the {@link JavaTypeRegistry}.
 *
 * @see Any
 * @see AnyKeyJavaType
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention( RUNTIME )
public @interface AnyKeyJavaClass {
	/**
	 * The Java Class
	 */
	Class<?> value();
}
