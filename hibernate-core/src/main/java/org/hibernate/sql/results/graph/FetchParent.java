/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.List;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;

/**
 * Contract for things that can be the parent of a fetch
 *
 * @author Steve Ebersole
 */
public interface FetchParent extends DomainResultGraphNode {
	/**
	 * This parent's mapping type
	 */
	FetchableContainer getReferencedMappingContainer();

	/**
	 * This parent's mapping type
	 */
	FetchableContainer getReferencedMappingType();

	/**
	 * Whereas {@link #getReferencedMappingContainer} and {@link #getReferencedMappingType} return the
	 * referenced container type, this method returns the referenced part.
	 *
	 * E.g. for a many-to-one this methods returns the
	 * {@link org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping} while
	 * {@link #getReferencedMappingContainer} and {@link #getReferencedMappingType} return the referenced
	 * {@link org.hibernate.metamodel.mapping.EntityMappingType}.
	 */
	default ModelPart getReferencedModePart() {
		return getReferencedMappingContainer();
	}

	/**
	 * Get the property path to this parent
	 */
	NavigablePath getNavigablePath();

	/**
	 * Retrieve the fetches owned by this fetch source.
	 */
	List<Fetch> getFetches();

	Fetch findFetch(String fetchableName);
}
