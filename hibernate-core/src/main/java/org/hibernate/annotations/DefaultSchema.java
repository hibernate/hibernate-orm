/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a default schema for all table mappings occurring within
 * the scope of the annotated package or type, overriding the effect of
 * {@value org.hibernate.cfg.AvailableSettings#DEFAULT_SCHEMA} and/or
 * {@value org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG} if
 * either of these configuration properties is specified.
 * <p>
 * The default schema specified by this annotation is ignored if a
 * table mapping explicitly specifies a
 * {@linkplain jakarta.persistence.Table#schema schema} or
 * {@linkplain jakarta.persistence.Table#catalog catalog}.
 *
 * @author Gavin King
 */
@Incubating
@Target({TYPE,PACKAGE})
@Retention(RUNTIME)
public @interface DefaultSchema {
	/**
	 * A schema to use when no schema is explicitly specified using
	 * {@link jakarta.persistence.Table#schema()}.
	 */
	String schema() default "";
	/**
	 * A catalog to use when no schema is explicitly specified using
	 * {@link jakarta.persistence.Table#catalog()}.
	 */
	String catalog() default "";
}
