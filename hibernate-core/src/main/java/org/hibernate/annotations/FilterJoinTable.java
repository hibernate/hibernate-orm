/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the join table of a collection is affected by a
 * named filter declared using {@link FilterDef}, and allows the
 * {@linkplain FilterDef#defaultCondition default filter condition}
 * to be overridden for the annotated entity or collection role.
 *
 * @author Emmanuel Bernard
 * @author Rob Worsnop
 *
 * @see Filter
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FilterJoinTables.class)
public @interface FilterJoinTable {
	/**
	 * The name of the filter declared using {@link FilterDef}.
	 */
	String name();

	/**
	 * The filter condition, a SQL expression used for filtering
	 * the rows returned by a query when the filter is enabled.
	 * If not specified, the default filter condition given by
	 * {@link FilterDef#defaultCondition} is used.
	 * <p>
	 * By default, aliases of filtered tables are automatically
	 * interpolated into the filter condition, before any token
	 * that looks like a column name. Occasionally, when the
	 * interpolation algorithm encounters ambiguity, the process
	 * of alias interpolation produces broken SQL. In such cases,
	 * alias interpolation may be controlled explicitly using
	 * either {@link #deduceAliasInjectionPoints} or
	 * {@link #aliases}.
	 */
	String condition() default "";

	/**
	 * Determines how tables aliases are interpolated into the
	 * {@link #condition} SQL expression.
	 * <ul>
	 * <li>if {@code true}, and by default, an alias is added
	 *     automatically to every column occurring in the SQL
	 *     expression, but
	 * <li>if {@code false}, aliases are only interpolated where
	 *     an explicit placeholder of form {@code {alias}} occurs
	 *     in the SQL expression.
	 * <li>Finally, if {@link #aliases explicit aliases} are
	 *     specified, then alias interpolation happens only for
	 *     the specified aliases.
	 * </ul>
	 */
	boolean deduceAliasInjectionPoints() default true;

	/**
	 * Explicitly specifies how aliases are interpolated into
	 * the {@link #condition} SQL expression. Each {@link
	 * SqlFragmentAlias} specifies a placeholder name and the
	 * table whose alias should be interpolated. Placeholders
	 * are of form {@code {name}} where {@code name} matches
	 * a {@link SqlFragmentAlias#alias}.
	 */
	SqlFragmentAlias[] aliases() default {};
}
