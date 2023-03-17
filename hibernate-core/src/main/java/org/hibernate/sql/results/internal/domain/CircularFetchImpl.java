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
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder;
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

	protected CircularFetchImpl(CircularFetchImpl original) {
		this.referencedModelPart = original.referencedModelPart;
		this.entityMappingType = original.entityMappingType;
		this.timing = original.timing;
		this.fetchParent = original.fetchParent;
		this.navigablePath = original.navigablePath;
		this.selectByUniqueKey = original.selectByUniqueKey;
		this.referencedNavigablePath = original.referencedNavigablePath;
		this.fetchable = original.fetchable;
		this.keyResult = original.keyResult;
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

		final Initializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				referencedModelPart,
				() -> initializerFactory( parentAccess, creationState )
		);

		return new BiDirectionalFetchAssembler(
				initializer.asEntityInitializer(),
				fetchable.getJavaType()
		);
	}

	protected Initializer initializerFactory(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		if ( timing == FetchTiming.IMMEDIATE ) {
			return buildEntitySelectFetchInitializer(
					parentAccess,
					fetchable,
					entityMappingType.getEntityPersister(),
					keyResult,
					getNavigablePath(),
					selectByUniqueKey,
					creationState
			);
		}
		else {
			return buildEntityDelayedFetchInitializer(
					parentAccess,
					getReferencedPath(),
					fetchable,
					selectByUniqueKey,
					keyResult.createResultAssembler( parentAccess, creationState )
			);
		}
	}

	protected Initializer buildEntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping fetchable,
			EntityPersister entityPersister,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			boolean selectByUniqueKey,
			AssemblerCreationState creationState) {
		return EntitySelectFetchInitializerBuilder
				.createInitializer(
						parentAccess,
						this.fetchable,
						entityPersister,
						keyResult,
						navigablePath,
						selectByUniqueKey,
						creationState
				);
	}

	protected Initializer buildEntityDelayedFetchInitializer(
			FetchParentAccess parentAccess,
			NavigablePath referencedPath,
			ToOneAttributeMapping fetchable,
			boolean selectByUniqueKey,
			DomainResultAssembler<?> resultAssembler) {
			return new EntityDelayedFetchInitializer(
					parentAccess,
					referencedPath,
					fetchable,
					selectByUniqueKey,
					resultAssembler
			);
		}


		@Override
		public FetchTiming getTiming ( ) {
			return timing;
		}

		@Override
		public boolean hasTableGroup ( ) {
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
