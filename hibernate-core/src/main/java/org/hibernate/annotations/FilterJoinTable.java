/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add filters to a join table collection.
 *
 * @author Emmanuel Bernard
 * @author Rob Worsnop
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FilterJoinTables.class)
public @interface FilterJoinTable {
	/**
	 * The filter name.
	 */
	String name();

	/**
	 * The filter condition.  If empty, the default condition from the correspondingly named {@link FilterDef} is used.
	 */
	String condition() default "";

	/**
	 * Do we need to determine all points within the condition fragment that are alias injection points?  Or
	 * are injection points already marked?
	 */
	boolean deduceAliasInjectionPoints() default true;

	/**
	 * The alias descriptors for injection.
	 */
	SqlFragmentAlias[] aliases() default {};
}
