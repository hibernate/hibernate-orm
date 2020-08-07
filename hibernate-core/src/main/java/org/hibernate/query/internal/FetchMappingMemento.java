/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface FetchMappingMemento {
	interface Parent {
		/**
		 * The path for the parent
		 */
		NavigablePath getNavigablePath();

		/**
		 * The entity descriptor that is the base for this path/parent
		 */
		ManagedMappingType getMappingType();
	}

	/**
	 * Resolve the fetch-memento into the result-graph-node builder
	 */
	FetchBuilder resolve(Parent parent, Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context);
}
