/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows <em>implicit polymorphism</em> to be disabled for
 * an entity class hierarchy, by annotating the root entity
 * {@code @Polymorphism(type=EXPLICIT)}.
 * <p>
 * Hibernate allows a query {@code from} clause to name a
 * {@linkplain org.hibernate.mapping.MappedSuperclass
 * mapped superclass}, or even an arbitrary Java type which
 * is neither an entity class nor a mapped superclass. The
 * query will return all entities which inherit the type.
 * For example, the query
 *     <pre>{@code from java.lang.Object}</pre>
 * will return every entity mapped by Hibernate!
 * <p>
 * This can be thought of as allowing a sort of "poor man's"
 * {@linkplain jakarta.persistence.InheritanceType#TABLE_PER_CLASS
 * table per class} inheritance, though it comes with many
 * limitations.
 * <p>
 * This annotation allows an entity class to refuse to
 * participate in such a crazy query, so that it's never
 * returned by any query that names one of its non-entity
 * supertypes.
 * <p>
 * Note that this annotation may only be applied to the root
 * entity in an entity inheritance hierarchy, and its effect
 * is inherited by entity subclasses.
 * <p>
 * Note also that this has <em>no effect at all</em> on the
 * usual polymorphism within a mapped entity class inheritance
 * hierarchy, as defied by the JPA specification. "Implicit"
 * polymorphism is about queries that span multiple such
 * entity inheritance hierarchies.
 * <p>
 * This annotation is hardly ever useful.
 *
 * @author Steve Ebersole
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface Polymorphism {
	/**
	 * Determines whether implicit polymorphism is enabled
	 * or disabled for the annotated entity class. It is
	 * enabled by default.
	 */
	PolymorphismType type() default PolymorphismType.IMPLICIT;
}
