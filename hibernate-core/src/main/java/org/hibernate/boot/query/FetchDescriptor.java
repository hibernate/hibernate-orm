/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.query;

import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.ResultMemento;

/**
 * Describes the mapping for a fetch as part of a {@link NamedResultSetMappingDescriptor}
 */
public interface FetchDescriptor {
	/**
	 * Resolve the descriptor into a memento capable of being stored in the
	 * {@link NamedObjectRepository}
	 */
	FetchMemento resolve(ResultSetMappingResolutionContext resolutionContext);

	ResultMemento asResultMemento(
			NavigablePath path,
			ResultSetMappingResolutionContext resolutionContext);
}
