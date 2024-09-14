/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.query;

import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;

/**
 * Describes the mapping for a result as part of a {@link NamedResultSetMappingDescriptor}
 *
 * @author Steve Ebersole
 */
public interface ResultDescriptor {
	/**
	 * Resolve the descriptor into a memento capable of being stored in the
	 * {@link NamedObjectRepository}
	 */
	ResultMemento resolve(ResultSetMappingResolutionContext resolutionContext);
}
