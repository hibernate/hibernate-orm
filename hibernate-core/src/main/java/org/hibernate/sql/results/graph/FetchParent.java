/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.BitSet;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;

/**
 * Contract for things that can be the parent of a fetch
 *
 * @author Steve Ebersole
 */
@Incubating
public interface FetchParent extends DomainResultGraphNode {
	/**
	 * This parent's mapping type
	 */
	FetchableContainer getReferencedMappingContainer();

	/**
	 * This parent's mapping type
	 */
	FetchableContainer getReferencedMappingType();

	default NavigablePath resolveNavigablePath(Fetchable fetchable) {
		final String fetchableName = fetchable.getFetchableName();
		if ( fetchable instanceof EntityIdentifierMapping ) {
			return new EntityIdentifierNavigablePath( getNavigablePath(), fetchableName );
		}
		else {
			final FetchableContainer referencedMappingContainer = getReferencedMappingContainer();
			final EntityMappingType fetchableEntityType = fetchable.findContainingEntityMapping();
			final EntityMappingType fetchParentType;
			if ( referencedMappingContainer instanceof EmbeddableMappingType
					|| referencedMappingContainer instanceof EmbeddableValuedModelPart ) {
				fetchParentType = referencedMappingContainer.findContainingEntityMapping();
			}
			else if ( referencedMappingContainer instanceof EntityMappingType ) {
				fetchParentType = (EntityMappingType) referencedMappingContainer;
			}
			else {
				fetchParentType = fetchableEntityType;
			}
			if ( fetchParentType != null && !fetchParentType.isTypeOrSuperType( fetchableEntityType ) ) {
				return getNavigablePath().treatAs( fetchableEntityType.getEntityName() )
						.append( fetchableName );
			}
			else {
				return getNavigablePath().append( fetchableName );
			}
		}
	}

	/**
	 * Whereas {@link #getReferencedMappingContainer} and {@link #getReferencedMappingType} return the
	 * referenced container type, this method returns the referenced part.
	 *
	 * E.g. for a many-to-one this method returns the
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
	ImmutableFetchList getFetches();

	Fetch findFetch(Fetchable fetchable);

	boolean hasJoinFetches();

	boolean containsCollectionFetches();

	default int getCollectionFetchesCount() {
		return getFetches().getCollectionFetchesCount();
	}

	@Override
	default void collectValueIndexesToCache(BitSet valueIndexes) {
		for ( Fetch fetch : getFetches() ) {
			fetch.collectValueIndexesToCache( valueIndexes );
		}
	}

	Initializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState);

	default FetchParent getRoot() {
		if ( this instanceof Fetch ) {
			return ( (Fetch) this ).getFetchParent().getRoot();
		}
		return this;
	}

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
