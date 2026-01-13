/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.sql.EmbeddedMapping;
import jakarta.persistence.sql.EntityMapping;
import jakarta.persistence.sql.FieldMapping;
import jakarta.persistence.sql.MemberMapping;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.named.ResultMementoEntity;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderEntityJpa;
import org.hibernate.query.results.internal.complete.DelayedFetchBuilderBasicPart;
import org.hibernate.query.results.internal.implicit.ImplicitFetchBuilderBasic;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetchable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
		this(
				new NavigablePath( entityDescriptor.getEntityName() ),
				entityDescriptor,
				lockMode,
				discriminatorMemento,
				explicitFetchMementoMap
		);
	}
	public ResultMementoEntityJpa(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			FetchMementoBasic discriminatorMemento,
			Map<String, FetchMemento> explicitFetchMementoMap) {
		this.navigablePath = navigablePath;
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.discriminatorMemento = discriminatorMemento;
		this.explicitFetchMementoMap = explicitFetchMementoMap;
	}

	public static <T> ResultMementoEntityJpa from(
			EntityMapping<T> entityMapping,
			SessionFactoryImplementor factory) {
		final EntityPersister entityDescriptor = factory.getMappingMetamodel()
				.getEntityDescriptor( entityMapping.entityClass() );
		final var rootPath = new NavigablePath( entityDescriptor.getRootPathName() );

		final FetchMementoBasic discriminatorMemento;
		if ( StringHelper.isEmpty( entityMapping.discriminatorColumn() ) ) {
			discriminatorMemento = null;
		}
		else {
			var discriminatorDescriptor = entityDescriptor.getDiscriminatorMapping();
			discriminatorMemento = new FetchMementoBasicStandard(
					rootPath.append( discriminatorDescriptor.getPartName() ),
					discriminatorDescriptor,
					entityMapping.discriminatorColumn()
			);
		}

		final Map<String, FetchMemento> members = CollectionHelper.mapOfSize( entityMapping.fields().length );
		for ( int i = 0; i < entityMapping.fields().length; i++ ) {
			final MemberMapping<?> memberMapping = entityMapping.fields()[i];
			if ( memberMapping instanceof FieldMapping<?,?> basicMapping ) {
				final BasicValuedModelPart modelPart = entityDescriptor
						.findSubPart( basicMapping.name() )
						.asBasicValuedModelPart();
				members.put( basicMapping.name(), new FetchMementoBasicStandard(
						rootPath.append( modelPart.getPartName() ),
						modelPart,
						basicMapping.columnName()
				) );
			}
			else {
				final EmbeddedMapping<?,?> embeddedMapping = (EmbeddedMapping<?, ?>) memberMapping;
				final EmbeddableValuedModelPart attributeMapping =
						(EmbeddableValuedModelPart) entityDescriptor.findSubPart( embeddedMapping.name() );
				members.put( embeddedMapping.name(), new FetchMementoEmbeddableStandard(
						rootPath.append( attributeMapping.getPartName() ),
						attributeMapping,
						extractEmbeddedColumns( embeddedMapping )
				) );
			}
		}

		return new ResultMementoEntityJpa(
				rootPath,
				entityDescriptor,
				LockMode.fromJpaLockMode( entityMapping.lockMode() ),
				discriminatorMemento,
				members
		);
	}

	private static List<String> extractEmbeddedColumns(EmbeddedMapping<?, ?> embeddedMapping) {
		final List<String> columnNames = new ArrayList<>();
		collectEmbeddedColumns( embeddedMapping, columnNames::add );
		return columnNames;
	}

	private static void collectEmbeddedColumns(
			EmbeddedMapping<?, ?> embeddedMapping,
			Consumer<String> columnNameConsumer) {
		for ( MemberMapping<?> memberMapping : embeddedMapping.fields() ) {
			if ( memberMapping instanceof FieldMapping<?,?> basicMapping ) {
				columnNameConsumer.accept( basicMapping.columnName() );
			}
			else {
				collectEmbeddedColumns( (EmbeddedMapping<?, ?>) memberMapping, columnNameConsumer );
			}
		}
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public Class<?> getResultJavaType() {
		return entityDescriptor.getJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {

		final HashMap<Fetchable, FetchBuilder> explicitFetchBuilderMap = new HashMap<>();

		// If there are no explicit fetches, we don't register DELAYED
		// builders to get implicit fetching of all basic fetchables
		if ( !explicitFetchMementoMap.isEmpty() ) {
			explicitFetchMementoMap.forEach(
					(relativePath, fetchMemento) -> explicitFetchBuilderMap.put(
							(Fetchable) entityDescriptor.findByPath( relativePath ),
							fetchMemento.resolve( this, querySpaceConsumer, context )
					)
			);

			final boolean isEnhancedForLazyLoading = entityDescriptor.getRepresentationStrategy().isBytecodeEnhanced();
			// Implicit basic fetches are DELAYED by default, so register
			// fetch builders for the remaining basic fetchables
			entityDescriptor.forEachAttributeMapping(
					attributeMapping -> {
						final var basicPart = attributeMapping.asBasicValuedModelPart();
						if ( basicPart != null ) {
							explicitFetchBuilderMap.computeIfAbsent(
									attributeMapping,
									k -> new DelayedFetchBuilderBasicPart(
											navigablePath.append( k.getFetchableName() ),
											basicPart,
											isEnhancedForLazyLoading
									)
							);
						}
					}
			);
		}

		return new CompleteResultBuilderEntityJpa(
				navigablePath,
				entityDescriptor,
				lockMode,
				discriminatorFetchBuilder( querySpaceConsumer, context ),
				explicitFetchBuilderMap
		);
	}

	@Override
	public <R> ResultSetMapping<R> toJpaMapping(SessionFactory sessionFactory) {
		//noinspection unchecked
		return new EntityMapping<>(
				(Class<R>) getResultJavaType(),
				lockMode.toJpaLockMode(),
				entityDescriptor.getDiscriminatorMapping() == null ? null : entityDescriptor.getDiscriminatorMapping().getSelectableName(),
				toJpaFieldMappings( sessionFactory )
		);
	}

	private static final MemberMapping<?>[] NO_MEMBERS = new MemberMapping<?>[0];

	private MemberMapping<?>[] toJpaFieldMappings(SessionFactory sessionFactory) {
		if ( CollectionHelper.isEmpty( explicitFetchMementoMap ) ) {
			return NO_MEMBERS;
		}

		var memberMappings = new MemberMapping<?>[ explicitFetchMementoMap.size() ];
		int index = 0;
		for ( Map.Entry<String, FetchMemento> entry : explicitFetchMementoMap.entrySet() ) {
			memberMappings[index++] = entry.getValue().toJpaMemberMapping( this, sessionFactory );
		}
		return memberMappings;
	}

	private FetchBuilderBasicValued discriminatorFetchBuilder(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final var discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		if ( discriminatorMapping == null || !entityDescriptor.hasSubclasses() ) {
			assert discriminatorMemento == null;
			return  null;
		}
		else {
			return discriminatorMemento == null
					? new ImplicitFetchBuilderBasic( navigablePath, discriminatorMapping )
					: (FetchBuilderBasicValued)
							discriminatorMemento.resolve( this, querySpaceConsumer, context );
		}
	}
}
