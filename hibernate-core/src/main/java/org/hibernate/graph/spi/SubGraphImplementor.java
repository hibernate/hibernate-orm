/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.spi;

import javax.persistence.Subgraph;

/**
 * Hibernate extension to the JPA entity-graph EntityGraph contract.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface SubGraphImplementor<T> extends AttributeNodeContainer, Subgraph<T> {
}
