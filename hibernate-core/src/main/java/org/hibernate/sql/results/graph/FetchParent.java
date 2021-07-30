/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.EntityIdentifierNavigablePath;
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

	default FetchParent resolveContainingAssociationParent() {
		final ModelPart referencedModePart = getReferencedModePart();
		if ( referencedModePart instanceof Association ) {
			return this;
		}
		if ( this instanceof Fetch ) {
			( (Fetch) this ).getFetchParent().resolveContainingAssociationParent();
		}
		return null;
	}

	default NavigablePath resolveNavigablePath(Fetchable fetchable) {
		final String fetchableName = fetchable.getFetchableName();
		if ( NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( fetchableName ) ) {
			return new EntityIdentifierNavigablePath( getNavigablePath(), fetchableName );
		}
		else {
			return getNavigablePath().append( fetchableName );
		}
	}

	/**
	 * Whereas {@link #getReferencedMappingContainer} and {@link #getReferencedMappingType} return the
	 * referenced container type, this method returns the referenced part.
	 *
	 * E.g. for a many-to-one this methods returns the
	 * {@link ToOneAttributeMapping} while
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

	Fetch findFetch(Fetchable fetchable);

	default Fetch generateFetchableFetch(
			Fetchable fetchable,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return fetchable.generateFetch(
				this,
				fetchablePath,
				fetchTiming,
				selected,
				resultVariable,
				creationState
		);
	}
}
