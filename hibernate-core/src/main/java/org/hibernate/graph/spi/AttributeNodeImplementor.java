/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface AttributeNodeImplementor<T> extends AttributeNode<T> {
	public Attribute<?,T> getAttribute();
	public AttributeNodeImplementor<T> makeImmutableCopy();
	
	public <X extends T> Subgraph<X> getSubgraph(boolean createIfNotPresent);
	public <X extends T> Subgraph<X> getSubgraph(Class<X> type, boolean createIfNotPresent);
	public <X extends T> Subgraph<X> getKeySubgraph(boolean createIfNotPresent);
	public <X extends T> Subgraph<X> getKeySubgraph(Class<X> type, boolean createIfNotPresent);
}
