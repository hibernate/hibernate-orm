/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import java.util.List;
import javax.persistence.AttributeNode;

/**
 * A container for AttributeNodeImplementors.  A "bridge"
 * between JPA's EntityGraph and Subgraph.
 *
 * @author Strong Liu <stliu@hibernate.org>
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface AttributeNodeContainer {
	List<AttributeNodeImplementor<?>> attributeNodes();
	List<AttributeNode<?>> jpaAttributeNodes();
	AttributeNodeImplementor findAttributeNode(String name);
}
