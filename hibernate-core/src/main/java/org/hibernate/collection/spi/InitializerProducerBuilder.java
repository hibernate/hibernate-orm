/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.ArrayInitializerProducer;
import org.hibernate.sql.results.graph.collection.internal.BagInitializerProducer;
import org.hibernate.sql.results.graph.collection.internal.ListInitializerProducer;
import org.hibernate.sql.results.graph.collection.internal.MapInitializerProducer;
import org.hibernate.sql.results.graph.collection.internal.SetInitializerProducer;

/**
 * @author Steve Ebersole
 */
@Incubating
public class InitializerProducerBuilder {
	public static CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			CollectionClassification classification,
			FetchParent fetchParent,
			boolean selected,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		return switch ( classification ) {
			case ARRAY ->
					createArrayInitializerProducer( navigablePath, attributeMapping, fetchParent, selected, indexFetch, elementFetch, creationState );
			case BAG, ID_BAG ->
					createBagInitializerProducer( navigablePath, attributeMapping, fetchParent, selected, elementFetch, creationState );
			case LIST ->
					createListInitializerProducer( navigablePath, attributeMapping, fetchParent, selected, indexFetch, elementFetch, creationState );
			case MAP, ORDERED_MAP, SORTED_MAP ->
					createMapInitializerProducer( navigablePath, attributeMapping, fetchParent, selected, indexFetch, elementFetch, creationState );
			case SET, ORDERED_SET, SORTED_SET ->
					createSetInitializerProducer( navigablePath, attributeMapping, fetchParent, selected, elementFetch, creationState );
		};
	}

	public static CollectionInitializerProducer createArrayInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		if ( indexFetch == null ) {
			indexFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getIndexDescriptor(),
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}
		if ( elementFetch == null ) {
			elementFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getElementDescriptor(),
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}

		return new ArrayInitializerProducer( attributeMapping, indexFetch, elementFetch );
	}

	public static CollectionInitializerProducer createBagInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			Fetch elementFetch,
			DomainResultCreationState creationState) {

		final Fetch idBagIdFetch;
		if ( attributeMapping.getIdentifierDescriptor() != null ) {
			idBagIdFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getIdentifierDescriptor(),
					navigablePath.append( CollectionPart.Nature.ID.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}
		else {
			idBagIdFetch = null;
		}

		if ( elementFetch == null ) {
			elementFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getElementDescriptor(),
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}

		return new BagInitializerProducer( attributeMapping, idBagIdFetch, elementFetch );
	}

	public static CollectionInitializerProducer createListInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		if ( indexFetch == null ) {
			indexFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getIndexDescriptor(),
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}
		if ( elementFetch == null ) {
			elementFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getElementDescriptor(),
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}

		return new ListInitializerProducer( attributeMapping, indexFetch, elementFetch );
	}

	public static CollectionInitializerProducer createMapInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		assert attributeMapping.getIndexDescriptor() != null;

		if ( indexFetch == null ) {
			indexFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getIndexDescriptor(),
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}

		if ( elementFetch == null ) {
			elementFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getElementDescriptor(),
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}

		return new MapInitializerProducer( attributeMapping, indexFetch, elementFetch );
	}

	public static CollectionInitializerProducer createSetInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		if ( elementFetch == null ) {
			elementFetch = fetchParent.generateFetchableFetch(
					attributeMapping.getElementDescriptor(),
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					FetchTiming.IMMEDIATE,
					selected,
					null,
					creationState
			);
		}
		return new SetInitializerProducer( attributeMapping, elementFetch );
	}

	public static CollectionInitializerProducer createCollectionTypeWrapperInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			CollectionClassification classification,
			FetchParent fetchParent,
			boolean selected,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		return switch ( classification ) {
			case ARRAY -> createArrayInitializerProducer(
					navigablePath,
					attributeMapping,
					fetchParent,
					selected,
					indexFetch,
					elementFetch,
					creationState
			);
			case BAG, ID_BAG -> {
				assert indexFetch == null;
				yield createBagInitializerProducer(
						navigablePath,
						attributeMapping,
						fetchParent,
						selected,
						elementFetch,
						creationState
				);
			}
			case LIST -> createListInitializerProducer(
					navigablePath,
					attributeMapping,
					fetchParent,
					selected,
					indexFetch,
					elementFetch,
					creationState
			);
			case MAP, ORDERED_MAP, SORTED_MAP -> createMapInitializerProducer(
					navigablePath,
					attributeMapping,
					fetchParent,
					selected,
					indexFetch,
					elementFetch,
					creationState
			);
			case SET, ORDERED_SET, SORTED_SET -> createSetInitializerProducer(
					navigablePath,
					attributeMapping,
					fetchParent,
					selected,
					elementFetch,
					creationState
			);
		};
	}

	private InitializerProducerBuilder() {
	}
}
