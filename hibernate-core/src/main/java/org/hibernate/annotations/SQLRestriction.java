/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a restriction written in native SQL to add to the generated
 * SQL for entities or collections.
 * <p>
 * For example, {@code @SQLRestriction} could be used to hide entity
 * instances which have been soft-deleted, either for the entity class
 * itself:
 * <pre>
 * &#64;Entity
 * &#64;SQLRestriction("status &lt;&gt; 'DELETED'")
 * class Document {
 *     ...
 *     &#64;Enumerated(STRING)
 *     Status status;
 *     ...
 * }
 * </pre>
 * <p>
 * or, at the level of an association to the entity:
 * <pre>
 * &#64;OneToMany(mappedBy = "owner")
 * &#64;SQLRestriction("status &lt;&gt; 'DELETED'")
 * List&lt;Document&gt; documents;
 * </pre>
 * <p>
 * The {@link SQLJoinTableRestriction} annotation lets a restriction be
 * applied to an {@linkplain jakarta.persistence.JoinTable association table}:
 * <pre>
 * &#64;ManyToMany
 * &#64;JoinTable(name = "collaborations")
 * &#64;SQLRestriction("status &lt;&gt; 'DELETED'")
 * &#64;SQLJoinTableRestriction("status = 'ACTIVE'")
 * List&lt;Document&gt; documents;
 * </pre>
 * <p>
 * By default, the restriction is applied to the primary table of the
 * entity. When the restriction involves columns of other tables, for
 * example, columns mapped by a superclass in a {@linkplain
 * jakarta.persistence.InheritanceType#JOINED joined inheritance}
 * hierarchy, the tables holding the columns must be identified using
 * {@linkplain #aliases alias placeholders} of the form {@code {name}}:
 * <pre>
 * &#64;OneToMany(mappedBy = "application")
 * &#64;SQLRestriction(value = "{version}.effective_to is null",
 *                 aliases = &#64;SqlFragmentAlias(alias = "version",
 *                                              entity = AbstractVersion.class))
 * List&lt;ApplicationVersion&gt; versions;
 * </pre>
 * <p>
 * Note that {@code @SQLRestriction}s are always applied and cannot be
 * disabled. Nor may they be parameterized. They're therefore <em>much</em>
 * less flexible than {@linkplain Filter filters}.
 *
 * @see Filter
 * @see DialectOverride.SQLRestriction
 * @see SQLJoinTableRestriction
 *
 * @since 6.3
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface SQLRestriction {
	/**
	 * A predicate, written in native SQL.
	 */
	String value();

	/**
	 * Explicitly specifies how aliases are interpolated into
	 * the {@link #value} SQL expression. Each {@link
	 * SqlFragmentAlias} specifies a placeholder name and the
	 * table whose alias should be interpolated. Placeholders
	 * are of form {@code {name}} where {@code name} matches
	 * a {@link SqlFragmentAlias#alias}.
	 *
	 * @since 8.1
	 */
	SqlFragmentAlias[] aliases() default {};
}
