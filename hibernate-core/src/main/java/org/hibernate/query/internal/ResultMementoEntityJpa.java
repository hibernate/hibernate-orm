/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.named.ResultMementoEntity;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderEntityJpa;
import org.hibernate.query.results.internal.complete.DelayedFetchBuilderBasicPart;
import org.hibernate.query.results.internal.implicit.ImplicitFetchBuilderBasic;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public class ResultMementoEntityJpa implements ResultMementoEntity, FetchMemento.Parent {
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final LockMode lockMode;
	private final FetchMementoBasic discriminatorMemento;
	private final Map<String, FetchMemento> explicitFetchMementoMap;

	public ResultMementoEntityJpa(
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			FetchMementoBasic discriminatorMemento,
			Map<String, FetchMemento> explicitFetchMementoMap) {
		this.navigablePath = new NavigablePath( entityDescriptor.getEntityName() );
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.discriminatorMemento = discriminatorMemento;
		this.explicitFetchMementoMap = explicitFetchMementoMap;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		final FetchBuilderBasicValued discriminatorFetchBuilder;
		if ( discriminatorMapping == null || !entityDescriptor.hasSubclasses() ) {
			assert discriminatorMemento == null;
			discriminatorFetchBuilder = null;
		}
		else {
			if ( discriminatorMemento != null ) {
				discriminatorFetchBuilder = (FetchBuilderBasicValued) discriminatorMemento.resolve( this, querySpaceConsumer, context );
			}
			else {
				discriminatorFetchBuilder = new ImplicitFetchBuilderBasic( navigablePath, discriminatorMapping );
			}
		}

		final HashMap<Fetchable, FetchBuilder> explicitFetchBuilderMap = new HashMap<>();

		// If there are no explicit fetches, we don't register DELAYED builders to get implicit fetching of all basic fetchables
		if ( !explicitFetchMementoMap.isEmpty() ) {
			explicitFetchMementoMap.forEach(
					(relativePath, fetchMemento) -> explicitFetchBuilderMap.put(
							(Fetchable) entityDescriptor.findByPath( relativePath ),
							fetchMemento.resolve( this, querySpaceConsumer, context )
					)
			);

			final boolean isEnhancedForLazyLoading = entityDescriptor.getRepresentationStrategy().isBytecodeEnhanced();
			// Implicit basic fetches are DELAYED by default, so register fetch builders for the remaining basic fetchables
			entityDescriptor.forEachAttributeMapping(
					attributeMapping -> {
						final BasicValuedModelPart basicPart = attributeMapping.asBasicValuedModelPart();
						if ( basicPart != null ) {
							final Function<Fetchable, FetchBuilder> fetchBuilderCreator = k -> new DelayedFetchBuilderBasicPart(
									navigablePath.append( k.getFetchableName() ),
									basicPart,
									isEnhancedForLazyLoading
							);
							explicitFetchBuilderMap.computeIfAbsent(
									attributeMapping,
									fetchBuilderCreator
							);
						}
					}
			);
		}

		return new CompleteResultBuilderEntityJpa(
				navigablePath,
				entityDescriptor,
				lockMode,
				discriminatorFetchBuilder,
				explicitFetchBuilderMap
		);
	}
}
