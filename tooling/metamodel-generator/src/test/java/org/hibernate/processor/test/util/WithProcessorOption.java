/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Hardy Ferentschik
 */
@Repeatable(WithProcessorOption.List.class)
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface WithProcessorOption {
	String key();

	String value();

	@Target({ METHOD, TYPE })
	@Retention(RUNTIME)
	@Documented
	@interface List {
		WithProcessorOption[] value();
	}
}
