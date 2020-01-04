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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Add filters to an entity or a target entity of a collection.
 *
 * @author Emmanuel Bernard
 * @author Matthew Inger
 * @author Magnus Sandberg
 * @author Rob Worsnop
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(Filters.class)
public @interface Filter {
	/**
	 * The filter name.
	 */
	String name();

	/**
	 * The filter condition.  If empty, the default condition from the correspondingly named {@link FilterDef} is used.
	 */
	String condition() default "";

	/**
	 * If true, automatically determine all points within the condition fragment that an alias should be injected.
	 * Otherwise, injection will only replace instances of explicit "{alias}" instances or
	 * {@link SqlFragmentAlias} descriptors.
	 */
	boolean deduceAliasInjectionPoints() default true;

	/**
	 * The alias descriptors for injection.
	 */
	SqlFragmentAlias[] aliases() default {};
}
