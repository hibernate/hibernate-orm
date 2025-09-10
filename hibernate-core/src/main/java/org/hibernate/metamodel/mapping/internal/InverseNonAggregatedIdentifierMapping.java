/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * The inverse part of a "non-aggregated" composite identifier.
 *
 * Exposes the virtual id embeddable as mapping type, which requires the attribute mapping to implement {@link NonAggregatedIdentifierMapping}.
 */
public class InverseNonAggregatedIdentifierMapping extends EmbeddedAttributeMapping implements NonAggregatedIdentifierMapping {
	private final IdClassEmbeddable idClassEmbeddable;
	private final EntityMappingType entityDescriptor;

	private final NonAggregatedIdentifierMapping.IdentifierValueMapper identifierValueMapper;

	// Constructor is only used for creating the inverse attribute mapping
	InverseNonAggregatedIdentifierMapping(
			ManagedMappingType keyDeclaringType,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			NonAggregatedIdentifierMapping inverseModelPart,
			EmbeddableMappingType embeddableTypeDescriptor,
			MappingModelCreationProcess creationProcess) {
		super(
				keyDeclaringType,
				declaringTableGroupProducer,
				selectableMappings,
				inverseModelPart,
				embeddableTypeDescriptor,
				creationProcess
		);

		entityDescriptor = inverseModelPart.findContainingEntityMapping();

		if ( inverseModelPart.getIdClassEmbeddable() == null ) {
			idClassEmbeddable = null;
			identifierValueMapper =
					(NonAggregatedIdentifierMapping.IdentifierValueMapper)
							super.getEmbeddableTypeDescriptor();
		}
		else {
			idClassEmbeddable =
					(IdClassEmbeddable)
							inverseModelPart.getIdClassEmbeddable().createInverseMappingType(
									this,
									declaringTableGroupProducer,
									selectableMappings,
									creationProcess
							);
			identifierValueMapper = idClassEmbeddable;
		}
	}

	@Override
	public Object instantiate() {
		return null;
	}

	@Override
	public String getPartName() {
		return super.getPartName();
	}

	@Override
	public Nature getNature() {
		return Nature.VIRTUAL;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return (EmbeddableMappingType) super.getPartMappingType();
	}
// --------------

	@Override
	public IdClassEmbeddable getIdClassEmbeddable() {
		return idClassEmbeddable;
	}

	@Override
	public VirtualIdEmbeddable getVirtualIdEmbeddable() {
		return (VirtualIdEmbeddable) getMappedType();
	}

	@Override
	public NonAggregatedIdentifierMapping.IdentifierValueMapper getIdentifierValueMapper() {
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
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
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
	public Object getIdentifier(Object entity) {
		return getIdentifier( entity, null );
	}

	@Override
	public Object getIdentifier(Object entity, MergeContext mergeContext) {
		if ( hasContainingClass() ) {
			final Object id = identifierValueMapper.getRepresentationStrategy().getInstantiator().instantiate( null );
			final var embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
			final Object[] propertyValues = new Object[embeddableTypeDescriptor.getNumberOfAttributeMappings()];
			for ( int i = 0; i < propertyValues.length; i++ ) {
				final var attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				final Object object = attributeMapping.getValue( entity );
				if ( object == null ) {
					final var idClassAttributeMapping = identifierValueMapper.getAttributeMapping( i );
					propertyValues[i] =
							idClassAttributeMapping.getPropertyAccess().getGetter().getReturnTypeClass().isPrimitive()
									? idClassAttributeMapping.getExpressibleJavaType().getDefaultValue()
									: null;
				}
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				else if ( attributeMapping instanceof ToOneAttributeMapping toOneAttributeMapping
						&& !( identifierValueMapper.getAttributeMapping( i ) instanceof ToOneAttributeMapping ) ) {
					final Object toOne = getIfMerged( object, mergeContext );
					final var targetPart =
							toOneAttributeMapping.getForeignKeyDescriptor()
									.getPart( toOneAttributeMapping.getSideNature().inverse() );
					if ( targetPart.isEntityIdentifierMapping() ) {
						propertyValues[i] =
								( (EntityIdentifierMapping) targetPart )
										.getIdentifier( toOne, mergeContext );
					}
					else {
						propertyValues[i] = toOne;
					}
				}
				else {
					propertyValues[i] = object;
				}
			}
			identifierValueMapper.setValues( id, propertyValues );
			return id;
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
		final Object[] propertyValues = new Object[identifierValueMapper.getNumberOfAttributeMappings()];
		final var embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
		for ( int position = 0; position < propertyValues.length; position++ ) {
			final var attribute = embeddableTypeDescriptor.getAttributeMapping( position );
			final var mappedIdAttributeMapping = identifierValueMapper.getAttributeMapping( position );
			Object object = mappedIdAttributeMapping.getValue( id );
			if ( attribute instanceof ToOneAttributeMapping toOneAttributeMapping
					&& !( mappedIdAttributeMapping instanceof ToOneAttributeMapping ) ) {
				final var entityPersister =
						toOneAttributeMapping.getEntityMappingType().getEntityPersister();
				final var entityKey = session.generateEntityKey( object, entityPersister );
				final var persistenceContext = session.getPersistenceContext();
				final var holder = persistenceContext.getEntityHolder( entityKey );
				// use the managed object i.e. proxy or initialized entity
				object = holder == null ? null : holder.getManagedObject();
				if ( object == null ) {
					object = entityDescriptor.findAttributeMapping( toOneAttributeMapping.getAttributeName() )
							.getValue( entity );
				}
			}
			propertyValues[position] = object;
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
