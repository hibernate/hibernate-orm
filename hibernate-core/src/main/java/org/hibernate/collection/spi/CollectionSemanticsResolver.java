/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;
import org.hibernate.mapping.Collection;

/**
 * Resolve the collection semantics for the given mapped collection.
 *
 * @apiNote Ideally, this would act as the contract that allows pluggable
 *          resolution of non-Java Collection types, perhaps as part of a
 *          generalized reflection on the attribute to determine its
 *          nature/classification
 *
 * @author Steve Ebersole
 */
@Incubating
public interface CollectionSemanticsResolver {
	// really need some form of access to the attribute site
	<CE,E> CollectionSemantics<CE,E> resolveRepresentation(Collection bootDescriptor);
}
