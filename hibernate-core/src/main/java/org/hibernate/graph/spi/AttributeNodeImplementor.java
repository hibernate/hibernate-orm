/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import java.util.Map;
import javax.persistence.AttributeNode;

import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;

/**
 * Hibernate extension to the JPA entity-graph AttributeNode contract.
 *
 * @author Strong Liu <stliu@hibernate.org>
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface AttributeNodeImplementor<T> extends AttributeNode<T> {
	/**
	 * @deprecated Use {@link #getPersistentAttribute()} instead
	 */
	@Deprecated
	PersistentAttribute<?,T> getAttribute();

	default PersistentAttribute<?, T> getPersistentAttribute() {
		return getAttribute();
	}

	Map<Class, SubGraphImplementor> subGraphs();
	Map<Class, SubGraphImplementor> keySubGraphs();

	AttributeNodeImplementor<T> makeImmutableCopy();

	SubGraphImplementor<T> extractSubGraph(PersistentAttribute<?,T> persistentAttribute);
}
