/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingFetch;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingResult;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * A "non-aggregated" composite identifier.
 *
 * This is an identifier defined using more than one {@link jakarta.persistence.Id}
 * attribute with zero-or-more {@link jakarta.persistence.MapsId}.
 *
 * Can also be a single {@link jakarta.persistence.Id} with {@link jakarta.persistence.MapsId}
 */
public class NonAggregatedIdentifierMappingImpl extends AbstractCompositeIdentifierMapping implements NonAggregatedIdentifierMapping {
	private final EntityPersister entityDescriptor;

	private final VirtualIdEmbeddable virtualIdEmbeddable;
	private final IdClassEmbeddable idClassEmbeddable;

	private final IdentifierValueMapper identifierValueMapper;

	public NonAggregatedIdentifierMappingImpl(
			EntityPersister entityPersister,
			RootClass bootEntityDescriptor,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		super( entityPersister, rootTableName, creationProcess );
		entityDescriptor = entityPersister;

		if ( bootEntityDescriptor.getIdentifierMapper() == null
				|| bootEntityDescriptor.getIdentifierMapper() == bootEntityDescriptor.getIdentifier() ) {
			// cid -> getIdentifier
			// idClass -> null
			final Component virtualIdSource = (Component) bootEntityDescriptor.getIdentifier();

			virtualIdEmbeddable = new VirtualIdEmbeddable(
					virtualIdSource,
					this,
					entityPersister,
					rootTableName,
					rootTableKeyColumnNames,
					creationProcess
			);
			idClassEmbeddable = null;
			identifierValueMapper = virtualIdEmbeddable;
		}
		else {
			// cid = getIdentifierMapper
			// idClass = getIdentifier
			final var virtualIdSource = bootEntityDescriptor.getIdentifierMapper();
			final var idClassSource = (Component) bootEntityDescriptor.getIdentifier();

			virtualIdEmbeddable = new VirtualIdEmbeddable(
					virtualIdSource,
					this,
					entityPersister,
					rootTableName,
					rootTableKeyColumnNames,
					creationProcess
			);
			idClassEmbeddable = new IdClassEmbeddable(
					idClassSource,
					bootEntityDescriptor,
					this,
					entityPersister,
					rootTableName,
					rootTableKeyColumnNames,
					virtualIdEmbeddable,
					creationProcess
			);
			identifierValueMapper = idClassEmbeddable;
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected NonAggregatedIdentifierMappingImpl(NonAggregatedIdentifierMappingImpl original) {
		super( original );
		entityDescriptor = original.entityDescriptor;
		virtualIdEmbeddable = original.virtualIdEmbeddable;
		idClassEmbeddable = original.idClassEmbeddable;
		identifierValueMapper = original.identifierValueMapper;
	}

	@Override
	public EmbeddableMappingType getMappedType() {
		return virtualIdEmbeddable;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return getMappedType();
	}

	@Override
	public IdClassEmbeddable getIdClassEmbeddable() {
		return idClassEmbeddable;
	}

	@Override
	public VirtualIdEmbeddable getVirtualIdEmbeddable() {
		return virtualIdEmbeddable;
	}

	@Override
	public IdentifierValueMapper getIdentifierValueMapper() {
		return identifierValueMapper;
	}

	@Override
	public boolean hasContainingClass() {
		return idClassEmbeddable != null;
	}

	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return identifierValueMapper;
	}

	@Override
	public boolean areEqual(@Nullable Object one, @Nullable Object other, SharedSessionContractImplementor session) {
		return identifierValueMapper.areEqual( one, other, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return identifierValueMapper.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		identifierValueMapper.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x, Y y, JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return identifierValueMapper.forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		if ( hasContainingClass() ) {
			final var selectableMappings = getEmbeddableTypeDescriptor();
			final List<ColumnReference> columnReferences = arrayList( selectableMappings.getJdbcTypeCount() );
			final var navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
			final var defaultTableReference =
					tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
			identifierValueMapper.forEachSelectable(
					0,
					(columnIndex, selection) -> {
						final var tableReference =
								defaultTableReference.resolveTableReference( selection.getContainingTableExpression() ) != null
										? defaultTableReference
										: tableGroup.resolveTableReference( navigablePath,
												selection.getContainingTableExpression() );
						final var columnReference =
								sqlAstCreationState.getSqlExpressionResolver()
										.resolveSqlExpression( tableReference, selection );
						columnReferences.add( (ColumnReference) columnReference );
					}
			);

			return new SqlTuple( columnReferences, this );
		}
		return super.toSqlExpression( tableGroup, clause, walker, sqlAstCreationState );
	}

	@Override
	public Nature getNature() {
		return Nature.VIRTUAL;
	}

	@Override
	public String getAttributeName() {
		return null;
	}

	@Override
	public Object getIdentifier(Object entity) {
		return getIdentifier( entity, null );
	}

	@Override
	public Object getIdentifier(Object entity, MergeContext mergeContext) {
		if ( hasContainingClass() ) {
			final var lazyInitializer = HibernateProxy.extractLazyInitializer( entity );
			if ( lazyInitializer != null ) {
				return lazyInitializer.getInternalIdentifier();
			}
			final var embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
			final var propertyValues = new Object[embeddableTypeDescriptor.getNumberOfAttributeMappings()];
			for ( int i = 0; i < propertyValues.length; i++ ) {
				final var attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				final Object o = attributeMapping.getValue( entity );
				if ( o == null ) {
					final var idClassAttributeMapping = identifierValueMapper.getAttributeMapping( i );
					propertyValues[i] =
							idClassAttributeMapping.getPropertyAccess().getGetter().getReturnTypeClass().isPrimitive()
									? idClassAttributeMapping.getExpressibleJavaType().getDefaultValue()
									: null;
				}
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				else if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping
						&& !( identifierValueMapper.getAttributeMapping( i ) instanceof ToOneAttributeMapping ) ) {
					final Object toOne = getIfMerged( o, mergeContext );
					final var targetPart =
							toOneAttributeMapping.getForeignKeyDescriptor()
									.getPart( toOneAttributeMapping.getSideNature().inverse() );
					propertyValues[i] =
							targetPart.isEntityIdentifierMapping()
									? ((EntityIdentifierMapping) targetPart).getIdentifier( toOne, mergeContext )
									: toOne;
				}
				else {
					propertyValues[i] = o;
				}
			}
			return identifierValueMapper.getRepresentationStrategy().getInstantiator().instantiate( () -> propertyValues );
		}
		else {
			return entity;
		}
	}

	private static Object getIfMerged(Object o, MergeContext mergeContext) {
		if ( mergeContext != null ) {
			final Object merged = mergeContext.get( o );
			if ( merged != null ) {
				return merged;
			}
		}
		return o;
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		final var propertyValues = new Object[identifierValueMapper.getNumberOfAttributeMappings()];
		final var embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
		for ( int i = 0; i < propertyValues.length; i++ ) {
			final var attribute = embeddableTypeDescriptor.getAttributeMapping( i );
			final var mappedIdAttributeMapping = identifierValueMapper.getAttributeMapping( i );
			Object object = mappedIdAttributeMapping.getValue( id );
			if ( attribute instanceof ToOneAttributeMapping toOneAttributeMapping
					&& !( mappedIdAttributeMapping instanceof ToOneAttributeMapping ) ) {
				final var entityPersister = toOneAttributeMapping.getEntityMappingType().getEntityPersister();
				final var entityKey = session.generateEntityKey( object, entityPersister );
				final var persistenceContext = session.getPersistenceContext();
				final var holder = persistenceContext.getEntityHolder( entityKey );
				// use the managed object i.e. proxy or initialized entity
				object = holder == null ? null : holder.getManagedObject();
				if ( object == null ) {
					// get the association out of the entity itself
					object = entityDescriptor.getPropertyValue(
							entity,
							toOneAttributeMapping.getAttributeName()
					);
					if ( object == null ) {
						if ( holder != null && holder.isEventuallyInitialized() ) {
							object = holder.getEntity();
						}
						else {
							object = session.internalLoad(
									entityPersister.getEntityName(),
									entityKey.getIdentifier(),
									true,
									true
							);
						}
					}
				}
			}
			propertyValues[i] = object;
		}
		embeddableTypeDescriptor.setValues( entity, propertyValues );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer, SharedSessionContractImplementor session) {
		return identifierValueMapper.breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		identifierValueMapper.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		identifierValueMapper.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new NonAggregatedIdentifierMappingResult<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new NonAggregatedIdentifierMappingFetch(
				fetchablePath,
				this,
				fetchParent,
				fetchTiming,
				selected,
				creationState
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableValuedFetchable

	@Override
	public String getSqlAliasStem() {
		return "id";
	}

	@Override
	public String getFetchableName() {
		return EntityIdentifierMapping.ID_ROLE_NAME;
	}

	@Override
	public int getNumberOfFetchables() {
		return getPartMappingType().getNumberOfFetchables();
	}

	@Override
	public Fetchable getFetchable(int position) {
		return getPartMappingType().getFetchable( position );
	}
}
