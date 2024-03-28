/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotating {@link ConcreteProxy} on the root entity class of an inheritance hierarchy
 * will allow types of that hierarchy to always produce proxies that resolve to the concrete
 * subtype class. This means both {@linkplain jakarta.persistence.FetchType#LAZY lazy associations}
 * and {@linkplain org.hibernate.Session#getReference plain references} can safely be used
 * with {@code instanceof} checks and type-casts.
 * <p>
 * Note that the table(s) of an entity <strong>might need to be accessed</strong> to
 * determine the concrete proxy type:
 * <ul>
 *     <li>
 *         With {@linkplain jakarta.persistence.InheritanceType#SINGLE_TABLE single table} inheritance,
 *         the {@linkplain jakarta.persistence.DiscriminatorColumn discriminator column} value
 *         will be {@code left join}ed when fetching associations or simply read from the entity
 *         table when getting references.
 *     </li>
 *     <li>
 *         When using {@linkplain jakarta.persistence.InheritanceType#JOINED joined} inheritance,
 *         all subtype tables will need to be {@code left join}ed to determine the concrete type.
 *         Note however that when using an explicit {@linkplain jakarta.persistence.DiscriminatorColumn
 *         discriminator column}, the behavior is the same as for single-table inheritance.
 *     </li>
 *     <li>
 *         Finally, for {@linkplain jakarta.persistence.InheritanceType#TABLE_PER_CLASS table-per-class}
 *         inheritance, all subtype tables will need to be (union) queried to determine the concrete type.
 *     </li>
 * </ul>
 *
 * @author Marco Belladelli
 * @since 6.6
 */
@Target( TYPE )
@Retention( RUNTIME )
@Incubating
public @interface ConcreteProxy {
}
