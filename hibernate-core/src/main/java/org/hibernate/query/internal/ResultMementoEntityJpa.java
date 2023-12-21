/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.query.results.BasicValuedFetchBuilder;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.complete.CompleteResultBuilderEntityJpa;
import org.hibernate.query.results.complete.DelayedFetchBuilderBasicPart;
import org.hibernate.query.results.implicit.ImplicitFetchBuilderBasic;
import org.hibernate.spi.NavigablePath;

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
		final BasicValuedFetchBuilder discriminatorFetchBuilder;
		if ( discriminatorMapping == null || !entityDescriptor.hasSubclasses() ) {
			assert discriminatorMemento == null;
			discriminatorFetchBuilder = null;
		}
		else {
			if ( discriminatorMemento != null ) {
				discriminatorFetchBuilder = (BasicValuedFetchBuilder) discriminatorMemento.resolve( this, querySpaceConsumer, context );
			}
			else {
				discriminatorFetchBuilder = new ImplicitFetchBuilderBasic( navigablePath, discriminatorMapping );
			}
		}

		final HashMap<String, FetchBuilder> explicitFetchBuilderMap = new HashMap<>();

		// If there are no explicit fetches, we don't register DELAYED builders to get implicit fetching of all basic fetchables
		if ( !explicitFetchMementoMap.isEmpty() ) {
			explicitFetchMementoMap.forEach(
					(relativePath, fetchMemento) -> explicitFetchBuilderMap.put(
							relativePath,
							fetchMemento.resolve( this, querySpaceConsumer, context )
					)
			);

			final boolean isEnhancedForLazyLoading = entityDescriptor.getRepresentationStrategy().isBytecodeEnhanced();
			// Implicit basic fetches are DELAYED by default, so register fetch builders for the remaining basic fetchables
			entityDescriptor.forEachAttributeMapping(
					attributeMapping -> {
						final BasicValuedModelPart basicPart = attributeMapping.asBasicValuedModelPart();
						if ( basicPart != null ) {
							final Function<String, FetchBuilder> fetchBuilderCreator = k -> new DelayedFetchBuilderBasicPart(
									navigablePath.append( k ),
									basicPart,
									isEnhancedForLazyLoading
							);
							explicitFetchBuilderMap.computeIfAbsent(
									attributeMapping.getFetchableName(),
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
