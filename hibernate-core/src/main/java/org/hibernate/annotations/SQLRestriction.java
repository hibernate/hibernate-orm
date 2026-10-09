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
 * If a restriction declared by an entity should be applied to a to-one
 * association to that entity type, the association should be mapped to
 * an {@linkplain jakarta.persistence.JoinTable association table}.
 * <pre>
 * &#64;ManyToOne
 * &#64;JoinTable(name = "application_document")
 * Document document;
 * </pre>
 * The {@code SQLRestriction} annotation may not be directly applied to
 * a field or property annotated {@link jakarta.persistence.OneToOne} or
 * {@link jakarta.persistence.ManyToOne}, and restrictions on foreign
 * key associations are dangerous.
 * <p>
 * The {@link SQLJoinTableRestriction} annotation lets a restriction be
 * applied to the columns of an association table:
 * <pre>
 * &#64;ManyToMany
 * &#64;JoinTable(name = "collaborations")
 * &#64;SQLRestriction("status &lt;&gt; 'DELETED'")
 * &#64;SQLJoinTableRestriction("status = 'ACTIVE'")
 * List&lt;Document&gt; documents;
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
}
