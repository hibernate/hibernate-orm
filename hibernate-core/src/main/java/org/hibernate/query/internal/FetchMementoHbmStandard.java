/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.results.complete.CompleteFetchBuilderEntityValuedModelPart;
import org.hibernate.query.results.dynamic.DynamicFetchBuilder;
import org.hibernate.query.results.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * @author Steve Ebersole
 */
public class FetchMementoHbmStandard implements FetchMemento, FetchMemento.Parent {

	private static final String ELEMENT_PREFIX = "element.";

	public interface FetchParentMemento {
		NavigablePath getNavigablePath();
		FetchableContainer getFetchableContainer();
	}

	private final NavigablePath navigablePath;
	private final String ownerTableAlias;
	private final String tableAlias;
	private final List<String> keyColumnNames;
	private final LockMode lockMode;
	private final FetchParentMemento parent;
	private final Map<String, FetchMemento> fetchMementoMap;
	private final Fetchable fetchable;

	public FetchMementoHbmStandard(
			NavigablePath navigablePath,
			String ownerTableAlias,
			String tableAlias,
			List<String> keyColumnNames,
			LockMode lockMode,
			FetchParentMemento parent,
			Map<String, FetchMemento> fetchMementoMap,
			Fetchable fetchable) {
		this.navigablePath = navigablePath;
		this.ownerTableAlias = ownerTableAlias;
		this.tableAlias = tableAlias;
		this.keyColumnNames = keyColumnNames;
		this.lockMode = lockMode;
		this.parent = parent;
		this.fetchMementoMap = fetchMementoMap;
		this.fetchable = fetchable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final Map<String, FetchBuilder> fetchBuilderMap = new HashMap<>();
		fetchMementoMap.forEach(
				(attrName, fetchMemento) -> fetchBuilderMap.put(
						attrName,
						fetchMemento.resolve(this, querySpaceConsumer, context )
				)
		);
		final DynamicResultBuilderEntityStandard resultBuilder;
		if ( fetchable instanceof PluralAttributeMapping ) {
			resultBuilder = new DynamicResultBuilderEntityStandard(
					(EntityMappingType) ( (PluralAttributeMapping) fetchable ).getElementDescriptor().getPartMappingType(),
					tableAlias,
					navigablePath
			);
			FetchBuilder element = fetchBuilderMap.get( "element" );
			if ( element != null ) {
				if ( element instanceof DynamicFetchBuilder ) {
					resultBuilder.addIdColumnAliases(
							( (DynamicFetchBuilder) element ).getColumnAliases().toArray( new String[0] )
					);
				}
				else {
					resultBuilder.addIdColumnAliases(
							( (CompleteFetchBuilderEntityValuedModelPart) element ).getColumnAliases().toArray( new String[0] )
					);
				}
			}
			FetchBuilder index = fetchBuilderMap.get( "index" );
			if ( index != null ) {
				resultBuilder.addFetchBuilder( CollectionPart.Nature.INDEX.getName(), index );
			}
			for ( Map.Entry<String, FetchBuilder> entry : fetchBuilderMap.entrySet() ) {
				if ( entry.getKey().startsWith( ELEMENT_PREFIX ) ) {
					resultBuilder.addFetchBuilder( entry.getKey().substring( ELEMENT_PREFIX.length() ), entry.getValue() );
				}
			}
		}
		else {
			resultBuilder = new DynamicResultBuilderEntityStandard(
					( (ToOneAttributeMapping) fetchable ).getEntityMappingType(),
					tableAlias,
					navigablePath
			);
			fetchBuilderMap.forEach( resultBuilder::addFetchBuilder );
		}
		return new DynamicFetchBuilderLegacy(
				tableAlias,
				ownerTableAlias,
				fetchable.getFetchableName(),
				keyColumnNames,
				fetchBuilderMap,
				resultBuilder
		);
	}
}
