/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal.domain;

import java.util.function.BiConsumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.internal.AbstractNonJoinedEntityFetch;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Andrea Boriero
 */
public class CircularFetchImpl extends AbstractNonJoinedEntityFetch implements BiDirectionalFetch {
	private final FetchTiming timing;
	private final NavigablePath referencedNavigablePath;

	public CircularFetchImpl(
			ToOneAttributeMapping referencedModelPart,
			FetchTiming timing,
			NavigablePath navigablePath,
			FetchParent fetchParent,
			boolean selectByUniqueKey,
			NavigablePath referencedNavigablePath,
			DomainResult<?> keyResult,
			DomainResultCreationState creationState) {
		super(
				navigablePath,
				referencedModelPart,
				fetchParent,
				keyResult,
				timing == FetchTiming.DELAYED && referencedModelPart.getEntityMappingType()
						.getEntityPersister()
						.isConcreteProxy(),
				selectByUniqueKey,
				creationState
		);
		this.timing = timing;
		this.referencedNavigablePath = referencedNavigablePath;
	}

	/**
	 * Used from Hibernate Reactive
	 */
	@SuppressWarnings("unused")
	protected CircularFetchImpl(CircularFetchImpl original) {
		super(
				original.getNavigablePath(),
				original.getFetchedMapping(),
				original.getFetchParent(),
				original.getKeyResult(),
				original.getDiscriminatorFetch(),
				original.isSelectByUniqueKey()
		);
		this.timing = original.timing;
		this.referencedNavigablePath = original.referencedNavigablePath;
	}

	@Override
	public NavigablePath getReferencedPath() {
		return referencedNavigablePath;
	}

	@Override
	public FetchTiming getTiming() {
		return timing;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new CircularFetchAssembler(
				getResultJavaType(),
				creationState.resolveInitializer( this, parent, this ).asEntityInitializer()
		);
	}

	@Override
	public EntityInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		if ( timing == FetchTiming.IMMEDIATE ) {
			return buildEntitySelectFetchInitializer(
					parent,
					getFetchedMapping(),
					getFetchedMapping().getEntityMappingType().getEntityPersister(),
					getKeyResult(),
					getNavigablePath(),
					isSelectByUniqueKey(),
					creationState
			);
		}
		else {
			return buildEntityDelayedFetchInitializer(
					parent,
					getNavigablePath(),
					getFetchedMapping(),
					isSelectByUniqueKey(),
					getKeyResult(),
					getDiscriminatorFetch(),
					creationState
			);
		}
	}

	protected EntityInitializer<?> buildEntitySelectFetchInitializer(
			InitializerParent<?> parent,
			ToOneAttributeMapping fetchable,
			EntityPersister entityPersister,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			boolean selectByUniqueKey,
			AssemblerCreationState creationState) {
		return EntitySelectFetchInitializerBuilder.createInitializer(
				parent,
				fetchable,
				entityPersister,
				keyResult,
				navigablePath,
				selectByUniqueKey,
				false,
				creationState
		);
	}

	protected EntityInitializer<?> buildEntityDelayedFetchInitializer(
			InitializerParent<?> parent,
			NavigablePath referencedPath,
			ToOneAttributeMapping fetchable,
			boolean selectByUniqueKey,
			DomainResult<?> keyResult,
			BasicFetch<?> discriminatorFetch,
			AssemblerCreationState creationState) {
		return new EntityDelayedFetchInitializer(
				parent,
				referencedPath,
				fetchable,
				selectByUniqueKey,
				keyResult,
				discriminatorFetch,
				creationState
		);
	}

	private static class CircularFetchAssembler implements DomainResultAssembler<Object> {
		private final EntityInitializer<InitializerData> initializer;
		private final JavaType<Object> assembledJavaType;

		public CircularFetchAssembler(JavaType<?> assembledJavaType, EntityInitializer<?> initializer) {
			//noinspection unchecked
			this.assembledJavaType = (JavaType<Object>) assembledJavaType;
			this.initializer = (EntityInitializer<InitializerData>) initializer;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState) {
			final InitializerData data = initializer.getData( rowProcessingState );
			final Initializer.State state = data.getState();
			if ( state == Initializer.State.KEY_RESOLVED ) {
				initializer.resolveInstance( data );
			}
			return initializer.getResolvedInstance( data );
		}

		@Override
		public void resolveState(RowProcessingState rowProcessingState) {
			initializer.resolveState( rowProcessingState );
		}

		@Override
		public @Nullable Initializer<?> getInitializer() {
			return initializer;
		}

		@Override
		public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
			if ( initializer.isResultInitializer() ) {
				consumer.accept( initializer, arg );
			}
		}

		@Override
		public JavaType<Object> getAssembledJavaType() {
			return assembledJavaType;
		}
	}

}
