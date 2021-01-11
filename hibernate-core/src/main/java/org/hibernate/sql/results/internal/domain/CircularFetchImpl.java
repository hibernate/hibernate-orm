/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class CircularFetchImpl implements BiDirectionalFetch, Association {
	private DomainResult keyResult;
	private EntityValuedModelPart referencedModelPart;
	private final EntityMappingType entityMappingType;
	private final FetchTiming timing;
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping fetchable;

	private final FetchParent fetchParent;
	private final NavigablePath referencedNavigablePath;

	public CircularFetchImpl(
			EntityValuedModelPart referencedModelPart,
			EntityMappingType entityMappingType,
			FetchTiming timing,
			NavigablePath navigablePath,
			FetchParent fetchParent,
			ToOneAttributeMapping fetchable,
			NavigablePath referencedNavigablePath,
			DomainResult keyResult) {
		this.referencedModelPart = referencedModelPart;
		this.entityMappingType = entityMappingType;
		this.timing = timing;
		this.fetchParent = fetchParent;
		this.navigablePath = navigablePath;
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
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final DomainResultAssembler resultAssembler = keyResult.createResultAssembler( creationState );

		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				referencedModelPart,
				() -> {
					if ( timing == FetchTiming.IMMEDIATE ) {
						return new EntitySelectFetchInitializer(
								parentAccess,
								(ToOneAttributeMapping) referencedModelPart,
								getReferencedPath(),
								entityMappingType.getEntityPersister(),
								resultAssembler,
								fetchable.isNullable()
						);
					}
					else {
						return new EntityDelayedFetchInitializer(
								getReferencedPath(),
								fetchable,
								resultAssembler
						);
					}
				}
		);

		return new BiDirectionalFetchAssembler(
				initializer,
				fetchable.getJavaTypeDescriptor()
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

	@Override
	public String getFetchableName() {
		return fetchable.getFetchableName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPartName() {
		return fetchable.getFetchableName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return fetchable.getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return fetchable.findContainingEntityMapping();
	}

	@Override
	public MappingType getPartMappingType() {
		return fetchable.getPartMappingType();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return ( (Association) fetchParent ).getForeignKeyDescriptor();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	private static class BiDirectionalFetchAssembler implements DomainResultAssembler {
		private EntityInitializer initializer;
		private JavaTypeDescriptor assembledJavaTypeDescriptor;

		public BiDirectionalFetchAssembler(
				EntityInitializer initializer,
				JavaTypeDescriptor assembledJavaTypeDescriptor) {
			this.initializer = initializer;
			this.assembledJavaTypeDescriptor = assembledJavaTypeDescriptor;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			return initializer.getInitializedInstance();
		}

		@Override
		public JavaTypeDescriptor getAssembledJavaTypeDescriptor() {
			return assembledJavaTypeDescriptor;
		}
	}

}
