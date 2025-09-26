/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.results.internal.complete.CompleteFetchBuilderEntityValuedModelPart;
import org.hibernate.query.results.internal.dynamic.DynamicFetchBuilder;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.internal.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * @author Steve Ebersole
 */
public class FetchMementoHbmStandard implements FetchMemento, FetchMemento.Parent {

	private static final String ELEMENT_PREFIX = "element.";
	private static final int ELEMENT_PREFIX_LENGTH = 8;

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
		if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			return resolve( pluralAttributeMapping, querySpaceConsumer, context );
		}
		else if ( fetchable instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			return resolve( toOneAttributeMapping, querySpaceConsumer, context );
		}
		else {
			throw new AssertionFailure( "Unexpected fetchable type" );
		}
	}

	private FetchBuilder resolve(
			PluralAttributeMapping pluralAttributeMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final DynamicResultBuilderEntityStandard resultBuilder;
		EntityMappingType partMappingType = (EntityMappingType) pluralAttributeMapping.getElementDescriptor()
				.getPartMappingType();
		resultBuilder = new DynamicResultBuilderEntityStandard(
				partMappingType,
				tableAlias,
				navigablePath
		);
		final Map<Fetchable, FetchBuilder> fetchBuilderMap = new HashMap<>();
		fetchMementoMap.forEach(
				(attrName, fetchMemento) -> {
					final FetchBuilder fetchBuilder = fetchMemento.resolve( this, querySpaceConsumer, context );

					if ( attrName.equals( "element" ) ) {
						if ( fetchBuilder instanceof DynamicFetchBuilder dynamicFetchBuilder ) {
							resultBuilder.addIdColumnAliases(
									dynamicFetchBuilder.getColumnAliases().toArray( new String[0] )
							);
						}
						else if ( fetchBuilder instanceof CompleteFetchBuilderEntityValuedModelPart completeFetchBuilder ) {
							resultBuilder.addIdColumnAliases(
									completeFetchBuilder.getColumnAliases().toArray( new String[0] )
							);
						}
						else {
							throw new AssertionFailure( "Unexpected fetch builder type" );
						}
						fetchBuilderMap.put(
								pluralAttributeMapping.getElementDescriptor(),
								fetchBuilder
						);
					}
					else if ( attrName.equals( "index" ) ) {
						final var indexDescriptor = pluralAttributeMapping.getIndexDescriptor();
						resultBuilder.addFetchBuilder( indexDescriptor, fetchBuilder );
						fetchBuilderMap.put( indexDescriptor, fetchBuilder );
					}
					else if ( attrName.startsWith( ELEMENT_PREFIX ) ) {
						final var attributeMapping =
								(Fetchable) partMappingType.findByPath( attrName.substring( ELEMENT_PREFIX_LENGTH ) );
						resultBuilder.addFetchBuilder( attributeMapping, fetchBuilder );
						fetchBuilderMap.put( attributeMapping, fetchBuilder );
					}
					else {
						final var attributeMapping = (Fetchable) partMappingType.findByPath( attrName );
						resultBuilder.addFetchBuilder( attributeMapping, fetchBuilder );
						fetchBuilderMap.put( attributeMapping, fetchBuilder );
					}
				}
		);
		return new DynamicFetchBuilderLegacy(
				tableAlias,
				ownerTableAlias,
				fetchable,
				keyColumnNames,
				fetchBuilderMap,
				resultBuilder
		);
	}

	private FetchBuilder resolve(
			ToOneAttributeMapping toOneAttributeMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final Map<Fetchable, FetchBuilder> fetchBuilderMap = new HashMap<>();
		fetchMementoMap.forEach(
				(attrName, fetchMemento) ->
						fetchBuilderMap.put(
								(Fetchable) toOneAttributeMapping.findSubPart( attrName ),
								fetchMemento.resolve( this, querySpaceConsumer, context )
						)
		);
		final var resultBuilder =
				new DynamicResultBuilderEntityStandard(
						toOneAttributeMapping.getEntityMappingType(),
						tableAlias,
						navigablePath
				);
		fetchBuilderMap.forEach( resultBuilder::addFetchBuilder );
		return new DynamicFetchBuilderLegacy(
				tableAlias,
				ownerTableAlias,
				fetchable,
				keyColumnNames,
				fetchBuilderMap,
				resultBuilder
		);
	}
}
