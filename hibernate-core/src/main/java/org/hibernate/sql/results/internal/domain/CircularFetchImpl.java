/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.from.TableGroup;
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
public class CircularFetchImpl implements BiDirectionalFetch, Association {
	private final DomainResult<?> keyResult;
	private final EntityValuedModelPart referencedModelPart;
	private final EntityMappingType entityMappingType;
	private final FetchTiming timing;
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping fetchable;
	private final boolean selectByUniqueKey;

	private final FetchParent fetchParent;
	private final NavigablePath referencedNavigablePath;

	public CircularFetchImpl(
			EntityValuedModelPart referencedModelPart,
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
	public JavaType<?> getResultJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final DomainResultAssembler resultAssembler = keyResult.createResultAssembler( parentAccess, creationState );

		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
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
									resultAssembler
							);
						}
						final EntityPersister entityPersister = entityMappingType.getEntityPersister();
						if ( entityPersister.isBatchLoadable() ) {
							return new BatchEntitySelectFetchInitializer(
									parentAccess,
									(ToOneAttributeMapping) referencedModelPart,
									getReferencedPath(),
									entityPersister,
									resultAssembler
							);
						}
						else {
							return new EntitySelectFetchInitializer(
									parentAccess,
									(ToOneAttributeMapping) referencedModelPart,
									getReferencedPath(),
									entityPersister,
									resultAssembler
							);
						}
					}
					else {
						return new EntityDelayedFetchInitializer(
								parentAccess,
								getReferencedPath(),
								fetchable,
								selectByUniqueKey,
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
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return fetchable.createDomainResult( navigablePath, tableGroup,resultVariable, creationState );
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
	public JavaType<?> getJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return ( (Association) fetchParent ).getForeignKeyDescriptor();
	}

	@Override
	public ForeignKeyDescriptor.Nature getSideNature() {
		return ( (Association) fetchParent ).getSideNature();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		fetchable.breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		throw new UnsupportedOperationException();
	}

	private static class BiDirectionalFetchAssembler implements DomainResultAssembler {
		private EntityInitializer initializer;
		private JavaType assembledJavaTypeDescriptor;

		public BiDirectionalFetchAssembler(
				EntityInitializer initializer,
				JavaType assembledJavaTypeDescriptor) {
			this.initializer = initializer;
			this.assembledJavaTypeDescriptor = assembledJavaTypeDescriptor;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			initializer.resolveInstance( rowProcessingState );
			return initializer.getInitializedInstance();
		}

		@Override
		public JavaType getAssembledJavaTypeDescriptor() {
			return assembledJavaTypeDescriptor;
		}
	}

}
