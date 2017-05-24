/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import javax.persistence.EntityGraph;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * Hibernate extension to the JPA entity-graph EntityGraph contract.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface EntityGraphImplementor<T> extends EntityGraph<T>, AttributeNodeContainer {
	boolean appliesTo(String entityName);

	boolean appliesTo(EntityDescriptor<? super T> entityType);

	/**
	 * Make a mutable copy of this entity graph
	 *
	 * @return The immutable copy
	 */
	EntityGraphImplementor<T> makeMutableCopy();

	/**
	 * Make an immutable copy of this entity graph, using the given name.
	 *
	 * @param name The name to apply to the immutable copy
	 *
	 * @return The immutable copy
	 */
	EntityGraphImplementor<T> makeImmutableCopy(String name);
}
