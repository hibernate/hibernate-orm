/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4.runtimecheck;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows to skip the Byteman rules to check whether forbidden APIs are invoked at runtime.
 * It makes sense only if the test is run with {@link BMRuntimeCheckCustomRunner}.
 *
 * @see BMRuntimeCheckCustomRunner
 * @author Fabio Massimo Ercoli
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface SkipBMRuntimeCheck {

	/**
	 * Comment describing the reason for the skip the rules.
	 *
	 * @return The comment
	 */
	String comment() default "";

}
