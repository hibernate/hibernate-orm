/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.BatchEntitySelectFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchByUniqueKeyInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Andrea Boriero
 */
public class CircularFetchImpl implements BiDirectionalFetch {
	private final DomainResult<?> keyResult;
	private final ToOneAttributeMapping referencedModelPart;
	private final EntityMappingType entityMappingType;
	private final FetchTiming timing;
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping fetchable;
	private final boolean selectByUniqueKey;

	private final FetchParent fetchParent;
	private final NavigablePath referencedNavigablePath;

	public CircularFetchImpl(
			ToOneAttributeMapping referencedModelPart,
			EntityMappingType entityMappingType,
			FetchTiming timing,
			NavigablePath navigablePath,
			FetchParent fetchParent,
			ToOneAttributeMapping fetchable,
			boolean selectByUniqueKey,
			NavigablePath referencedNavigablePath,
			DomainResult<?> keyResult) {
		this.referencedModelPart = referencedModelPart;
		this.entityMappingType = entityMappingType;
		this.timing = timing;
		this.fetchParent = fetchParent;
		this.navigablePath = navigablePath;
		this.selectByUniqueKey = selectByUniqueKey;
		this.referencedNavigablePath = referencedNavigablePath;
		this.fetchable = fetchable;
		this.keyResult = keyResult;

	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigablePath getReferencedPath() {
		return referencedNavigablePath;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return fetchable;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return fetchable.getJavaType();
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final DomainResultAssembler<?> keyAssembler = keyResult.createResultAssembler( parentAccess, creationState );

		final Initializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				referencedModelPart,
				() -> {
					if ( timing == FetchTiming.IMMEDIATE ) {
						if ( selectByUniqueKey ) {
							return new EntitySelectFetchByUniqueKeyInitializer(
									parentAccess,
									fetchable,
									getNavigablePath(),
									entityMappingType.getEntityPersister(),
									keyAssembler
							);
						}
						final EntityPersister entityPersister = entityMappingType.getEntityPersister();
						if ( entityPersister.isBatchLoadable() ) {
							if ( parentAccess.isEmbeddableInitializer() ) {
								return new BatchEntityInsideEmbeddableSelectFetchInitializer(
										parentAccess,
										referencedModelPart,
										getNavigablePath(),
										entityPersister,
										keyResult.createResultAssembler( parentAccess, creationState )
								);
							}
							return new BatchEntitySelectFetchInitializer(
									parentAccess,
									referencedModelPart,
									getReferencedPath(),
									entityPersister,
									keyAssembler
							);
						}
						else {
							return new EntitySelectFetchInitializer(
									parentAccess,
									(ToOneAttributeMapping) referencedModelPart,
									getReferencedPath(),
									entityPersister,
									keyAssembler
							);
						}
					}
					else {
						return new EntityDelayedFetchInitializer(
								parentAccess,
								getReferencedPath(),
								fetchable,
								selectByUniqueKey,
								keyAssembler
						);
					}
				}
		);

		return new BiDirectionalFetchAssembler(
				initializer.asEntityInitializer(),
				fetchable.getJavaType()
		);
	}

	@Override
	public FetchTiming getTiming() {
		return timing;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	private static class BiDirectionalFetchAssembler implements DomainResultAssembler {
		private EntityInitializer initializer;
		private JavaType assembledJavaType;

		public BiDirectionalFetchAssembler(
				EntityInitializer initializer,
				JavaType assembledJavaType) {
			this.initializer = initializer;
			this.assembledJavaType = assembledJavaType;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			initializer.resolveInstance( rowProcessingState );
			return initializer.getInitializedInstance();
		}

		@Override
		public JavaType getAssembledJavaType() {
			return assembledJavaType;
		}
	}

}
