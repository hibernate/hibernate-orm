/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * SQL when querying an entity or collection.
 * <p>
 * For example, {@code @Where} could be used to hide entity instances which
 * have been soft-deleted, either for the entity class itself:
 * <pre>
 * &#64;Entity
 * &#64;Where(clause = "status &lt;&gt; 'DELETED'")
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
 * &#64;Where(clause = "status &lt;&gt; 'DELETED'")
 * List&lt;Document&gt; documents;
 * </pre>
 * <p>
 * The {@link WhereJoinTable} annotation lets a restriction be applied to
 * an {@linkplain jakarta.persistence.JoinTable association table}:
 * <pre>
 * &#64;ManyToMany
 * &#64;JoinTable(name = "collaborations")
 * &#64;Where(clause = "status &lt;&gt; 'DELETED'")
 * &#64;WhereJoinTable(clause = "status = 'ACTIVE'")
 * List&lt;Document&gt; documents;
 * </pre>
 * <p>
 * By default, {@code @Where} restrictions declared for an entity are also
 * applied when loading associations of that entity type. This behavior can
 * be disabled using the setting
 * {@value org.hibernate.cfg.AvailableSettings#USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS}.
 * However, this setting is now deprecated.
 * <p>
 * Note that {@code @Where} restrictions are always applied and cannot be
 * disabled. Nor may they be parameterized. They're therefore <em>much</em>
 * less flexible than {@linkplain Filter filters}.
 *
 * @see Filter
 * @see DialectOverride.Where
 * @see org.hibernate.cfg.AvailableSettings#USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS
 * @see WhereJoinTable
 *
 * @author Emmanuel Bernard
 *
 * @deprecated Use {@link SQLRestriction}
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated(since = "6.3")
public @interface Where {
	/**
	 * A predicate, written in native SQL.
	 */
	String clause();
}
