/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Filter definition.  Defines a name, default condition and parameter types (if any).
 *
 * @author Matthew Inger
 * @author Emmanuel Bernard
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(FilterDefs.class)
public @interface FilterDef {
	/**
	 * The filter name.
	 */
	String name();

	/**
	 * The default filter condition.
	 */
	String defaultCondition() default "";

	/**
	 * The filter parameter definitions.
	 */
	ParamDef[] parameters() default {};
}
