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
 * Specifies an {@code on delete} action for a foreign key constraint.
 * The most common usage is {@code @OnDelete(action = CASCADE)}.
 * <pre>
 * &#064;ManyToOne
 * &#064;OnDelete(action = CASCADE)
 * Parent parent;
 * </pre>
 * This code results in an {@code on delete cascade} clause in the DDL
 * definition of the foreign key.
 * <p>
 * The {@code @OnDelete} annotation may be applied to any field or
 * property representing an association or collection, or to a subclass
 * in a {@linkplain jakarta.persistence.InheritanceType#JOINED joined}
 * inheritance hierarchy.
 * <pre>
 * &#064;Entity
 * &#064;Inheritance(strategy = JOINED)
 * class Publication {
 *     &#064;Id
 *     long id;
 *     ...
 *     &#064;ElementCollection
 *     &#064;OnDelete(action = CASCADE)
 *     String&lt;String&gt; keywords;
 * }
 *
 * &#064;Entity
 * &#064;OnDelete(action = CASCADE)
 * class Book extends Publication {
 *     &#064;Column(unique = true);
 *     String isbn;
 *     ...
 *     &#064;ManyToMany
 *     &#064;OnDelete(action = CASCADE)
 *     Set&lt;Author&gt; authors;
 * }
 * </pre>
 * <p>
 * The affect of {@code @OnDelete(action = CASCADE)} is quite different
 * to {@link jakarta.persistence.CascadeType#REMOVE}. It's more efficient
 * to delete a row via {@code on delete cascade}, but there's a catch.
 * Like database triggers, {@code on delete} actions can cause state held
 * in memory to lose synchronization with the database. In particular,
 * when an entity instance is deleted via {@code on delete cascade}, the
 * instance might not be removed from the second-level cache.
 * <p>
 * To alleviate this problem, {@code @OnDelete} may be used together with
 * {@code cascade=REMOVE}.
 * <pre>
 * &#064;OneToMany(mappedBy = Child_.parent, cascade = {PERSIST, REMOVE})
 * &#064;OnDelete(action = CASCADE)
 * Set&lt;Child&gt; children = new HashSet&lt;&gt;();
 * </pre>
 * This mapping looks redundant, but it's not.
 * <ul>
 * <li>If {@code @OnDelete(action = CASCADE)} is used in conjunction
 *     with {@code cascade=REMOVE}, then associated entities are fetched
 *     from the database, marked deleted in the persistence context,
 *     and evicted from the second-level cache.
 * <li>If {@code @OnDelete(action = CASCADE)} is used on its own,
 *     <em>without</em> {@code cascade=REMOVE}, then associated
 *     entities are not fetched from the database, are not marked
 *     deleted in the persistence context, and are not automatically
 *     evicted from the second-level cache.
 * </ul>
 * <p>
 * Other options such as {@link OnDeleteAction#SET_NULL} and
 * {@link OnDeleteAction#SET_DEFAULT} are much less commonly used.
 * Note that {@code @OnDelete(SET_DEFAULT)} should be used together
 * with {@link ColumnDefault @ColumnDefault}.
 * <pre>
 * &#064;ManyToOne
 * &#064;OnDelete(action = OnDeleteAction.SET_DEFAULT)
 * &#064;ColumnDefault("-1")
 * Parent parent;
 * </pre>
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
public @interface OnDelete {
	/**
	 * The action to taken by the database when deletion of a row
	 * would cause the constraint to be violated.
	 */
	OnDeleteAction action();
}
