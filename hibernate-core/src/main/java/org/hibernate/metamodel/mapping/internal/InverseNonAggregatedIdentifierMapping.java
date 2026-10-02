package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

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
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;

import jakarta.annotation.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;


/**
 * The inverse part of a "non-aggregated" composite identifier.
 *
 * Exposes the virtual id embeddable as mapping type, which requires the attribute mapping to implement {@link NonAggregatedIdentifierMapping}.
 */
public class InverseNonAggregatedIdentifierMapping extends EmbeddedAttributeMapping implements NonAggregatedIdentifierMapping {
	@Nullable private final IdClassEmbeddable idClassEmbeddable;
	private final EntityMappingType entityDescriptor;

	private final NonAggregatedIdentifierMapping.IdentifierValueMapper identifierValueMapper;

	// Constructor is only used for creating the inverse attribute mapping
	InverseNonAggregatedIdentifierMapping(
			@Nullable ManagedMappingType keyDeclaringType,
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

		entityDescriptor = castNonNull( inverseModelPart.findContainingEntityMapping() );

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

	@Nullable
	@Override
	public Object instantiate() {
		return null;
	}

	@Nonnull
	@Override
	public String getPartName() {
		return castNonNull( super.getPartName() );
	}

	@Nonnull
	@Override
	public Nature getNature() {
		return Nature.VIRTUAL;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getPartMappingType() {
		return (EmbeddableMappingType) super.getPartMappingType();
	}
// --------------

	@Nullable
	@Override
	public IdClassEmbeddable getIdClassEmbeddable() {
		return idClassEmbeddable;
	}

	@Nonnull
	@Override
	public VirtualIdEmbeddable getVirtualIdEmbeddable() {
		return (VirtualIdEmbeddable) getMappedType();
	}

	@Nonnull
	@Override
	public NonAggregatedIdentifierMapping.IdentifierValueMapper getIdentifierValueMapper() {
		return identifierValueMapper;
	}

	@Override
	public boolean hasContainingClass() {
		return idClassEmbeddable != null;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return identifierValueMapper;
	}

	@Override
	public boolean areEqual(@Nullable Object one, @Nullable Object other, @Nullable SharedSessionContractImplementor session) {
		return identifierValueMapper.areEqual( one, other, session );
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return identifierValueMapper.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		identifierValueMapper.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return identifierValueMapper.forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Nonnull
	@Override
	public SqlTuple toSqlExpression(
			@Nonnull TableGroup tableGroup,
			@Nonnull Clause clause,
			@Nonnull SqmToSqlAstConverter walker,
			@Nonnull SqlAstCreationState sqlAstCreationState) {
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

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity) {
		return getIdentifier( entity, null );
	}

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity, @Nullable MergeContext mergeContext) {
		if ( hasContainingClass() ) {
			final Object id = identifierValueMapper.getRepresentationStrategy().getInstantiator().instantiate( null );
			final var embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
			final var propertyValues = new Object[embeddableTypeDescriptor.getNumberOfAttributeMappings()];
			for ( int i = 0; i < propertyValues.length; i++ ) {
				final var attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				final Object object = attributeMapping.getValue( entity );
				if ( object == null ) {
					final var idClassAttributeMapping = identifierValueMapper.getAttributeMapping( i );
					propertyValues[i] =
							castNonNull( idClassAttributeMapping.getPropertyAccess() ).getGetter().getReturnTypeClass().isPrimitive()
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

	private static Object getIfMerged(Object o, @Nullable MergeContext mergeContext) {
		if ( mergeContext != null ) {
			final Object merged = mergeContext.get( o );
			if ( merged != null ) {
				return merged;
			}
		}
		return o;
	}

	@Override
	public void setIdentifier(@Nonnull Object entity, @Nullable Object id, @Nonnull SharedSessionContractImplementor session) {
		final var propertyValues = new Object[identifierValueMapper.getNumberOfAttributeMappings()];
		final var embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
		for ( int position = 0; position < propertyValues.length; position++ ) {
			final var attribute = embeddableTypeDescriptor.getAttributeMapping( position );
			final var mappedIdAttributeMapping = identifierValueMapper.getAttributeMapping( position );
			Object object = id == null ? mappedIdAttributeMapping.getExpressibleJavaType().getDefaultValue()
					: mappedIdAttributeMapping.getValue( id );
			if ( object != null && attribute instanceof ToOneAttributeMapping toOneAttributeMapping
					&& !( mappedIdAttributeMapping instanceof ToOneAttributeMapping ) ) {
				final var entityPersister =
						toOneAttributeMapping.getEntityMappingType().getEntityPersister();
				final var entityKey = session.generateEntityKey( object, entityPersister );
				final var persistenceContext = session.getPersistenceContext();
				final var holder = persistenceContext.getEntityHolder( entityKey );
				// use the managed object i.e. proxy or initialized entity
				object = holder == null ? null : holder.getManagedObject();
				if ( object == null ) {
					object = castNonNull( entityDescriptor.findAttributeMapping( toOneAttributeMapping.getAttributeName() ) )
							.getValue( entity );
				}
			}
			propertyValues[position] = object;
		}
		embeddableTypeDescriptor.setValues( entity, propertyValues );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer, @Nullable SharedSessionContractImplementor session) {
		return identifierValueMapper.breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		identifierValueMapper.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		identifierValueMapper.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableValuedFetchable

	@Override
	public String getSqlAliasStem() {
		return "id";
	}

	@Nonnull
	@Override
	public String getFetchableName() {
		return EntityIdentifierMapping.ID_ROLE_NAME;
	}

	@Override
	public int getNumberOfFetchables() {
		return getPartMappingType().getNumberOfFetchables();
	}

	@Nonnull
	@Override
	public Fetchable getFetchable(int position) {
		return getPartMappingType().getFetchable( position );
	}
}
