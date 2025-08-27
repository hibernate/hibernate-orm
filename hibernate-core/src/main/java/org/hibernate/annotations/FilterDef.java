/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares a filter, specifying its {@linkplain #name}, optionally,
 * a {@linkplain #defaultCondition() default condition}, and its
 * {@linkplain #parameters parameter names and types}, if it has
 * parameters.
 * <p>
 * Every entity or collection which is affected by a named filter
 * declared using this annotation must be explicitly annotated
 * {@link Filter @Filter}, and the name of this filter definition
 * must be {@linkplain Filter#name given}. The {@code @Filter}
 * annotation may override the default condition specified by this
 * annotation using {@link Filter#condition}.
 * <p>
 * For example, if a filter is declared as follows:
 * <pre>
 * &#64;FilterDef(name = "Current",
 *            defaultCondition = "status&lt;&gt;'DELETED'")
 * package org.hibernate.domain;
 * </pre>
 * <p>
 * Then the filter may be applied to an entity type like this:
 * <pre>
 * &#64;Entity
 * &#64;Filter(name = "Current")
 * class Record {
 *     &#64;Id @GeneratedValue Long id;
 *     &#64;Enumerated(STRING) Status status;
 *     ...
 * }
 * </pre>
 * <p>
 * At runtime, a filter may be enabled in a particular session by
 * calling {@link org.hibernate.Session#enableFilter(String)},
 * passing the name of the filter, and then supplying arguments to
 * its parameters.
 * <pre>
 * session.enableFilter("Current");
 * </pre>
 * <p>
 * A filter has no effect unless:
 * <ul>
 * <li>it is explicitly enabled by calling {@code enableFilter}, or
 * <li>it is declared {@link #autoEnabled autoEnabled = true}.
 * </ul>
 *
 * @author Matthew Inger
 * @author Emmanuel Bernard
 *
 * @see org.hibernate.Filter
 * @see DialectOverride.FilterDefs
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(FilterDefs.class)
public @interface FilterDef {
	/**
	 * The name of the declared filter. Must be unique within a
	 * persistence unit.
	 *
	 * @see org.hibernate.SessionFactory#getDefinedFilterNames()
	 */
	String name();

	/**
	 * The default filter condition, a SQL expression used for
	 * filtering the rows returned by a query when the filter is
	 * enabled. This default condition may be overridden by any
	 * entity or collection to which the filter applies using
	 * {@link Filter#condition}.
	 * <p>
	 * If every entity and collection to which the filter applies
	 * explicitly specifies its own filter condition, then the
	 * default condition is unnecessary, and so this member is
	 * optional.
	 */
	String defaultCondition() default "";

	/**
	 * The names and types of the parameters of the filter.
	 */
	ParamDef[] parameters() default {};

	/**
	 * Specifies that the filter is auto-enabled, so that it is
	 * not necessary to call
	 * {@link org.hibernate.Session#enableFilter(String)}.
	 * <p>
	 * Arguments to {@linkplain #parameters} of auto-enabled
	 * filters are supplied via {@link ParamDef#resolver}.
	 */
	boolean autoEnabled() default false;

	/**
	 * Specifies that the filter should be applied to operations
	 * which fetch an entity by its identifier.
	 * <p>
	 * By default, a filter does not apply to lookups by primary
	 * key, for example, when:
	 * <ul>
	 * <li>{@linkplain org.hibernate.FetchMode fetching} a
	 *     {@code @ManyToOne} association, or
	 * <li>{@link org.hibernate.Session#find(Class, Object) find()}
	 *     is called.
	 * </ul>
	 * <p>
	 * If the effect of a filter with {@code applyToLoadByKey = true}
	 * would be to nullify a to-one association,
	 * {@link org.hibernate.EntityFilterException} is thrown.
	 */
	@Incubating
	boolean applyToLoadByKey() default false;
}
