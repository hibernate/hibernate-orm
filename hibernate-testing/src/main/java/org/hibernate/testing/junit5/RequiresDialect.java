/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

/**
 * Indicates that the annotated test class/method should only
 * be run when the indicated Dialect is being used.
 *
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface RequiresDialect {
	/**
	 * The Dialect class to match.
	 */
	Class<? extends Dialect> dialectClass();

	/**
	 * Should subtypes of {@link #dialectClass()} be matched?
	 */
	boolean matchSubTypes() default false;
}
