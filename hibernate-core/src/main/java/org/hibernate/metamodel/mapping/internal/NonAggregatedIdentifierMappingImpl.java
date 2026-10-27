package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiFunction;

import org.hibernate.type.descriptor.java.JavaType;

import jakarta.annotation.Nonnull;

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
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingFetch;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingResult;

import jakarta.annotation.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;


/**
 * A "non-aggregated" composite identifier.
 *
 * This is an identifier defined using more than one {@link jakarta.persistence.Id}
 * attribute with zero-or-more {@link jakarta.persistence.MapsId}.
 *
 * Can also be a single {@link jakarta.persistence.Id} with {@link jakarta.persistence.MapsId}
 */
public class NonAggregatedIdentifierMappingImpl<E, I> extends AbstractCompositeIdentifierMapping<E, E, I> implements NonAggregatedIdentifierMapping<E, I> {
	@Override
	public JavaType<E> getJavaType() {
		return virtualIdEmbeddable.getJavaType();
	}

	private final EntityPersister<E> entityDescriptor;

	private final VirtualIdEmbeddable<E> virtualIdEmbeddable;
	@Nullable private final IdClassEmbeddable<I> idClassEmbeddable;

	private final IdentifierValueMapper<I> identifierValueMapper;

	public static <E> NonAggregatedIdentifierMappingImpl<E, ?> create(
			EntityPersister<E> entityPersister,
			RootClass bootEntityDescriptor,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		final var identifierMapper = bootEntityDescriptor.getIdentifierMapper();
		final var identifier = bootEntityDescriptor.getIdentifier();
		if ( identifierMapper == null || identifierMapper == identifier ) {
			return new NonAggregatedIdentifierMappingImpl<E, E>( entityPersister,
					(Component) identifier, rootTableName, rootTableKeyColumnNames, creationProcess,
					(mapping, virtualId) -> virtualId );
		}
		final var idClassSource = (Component) identifier;
		return createWithIdClass( entityPersister, bootEntityDescriptor, identifierMapper,
				idClassSource, rootTableName, rootTableKeyColumnNames, creationProcess,
				creationProcess.getCreationContext().getTypeConfiguration().getJavaTypeRegistry()
						.resolveManagedTypeDescriptor( idClassSource.getComponentClass() ) );
	}

	private static <E, I> NonAggregatedIdentifierMappingImpl<E, I> createWithIdClass(
			EntityPersister<E> entityPersister,
			RootClass bootEntityDescriptor,
			Component virtualIdSource,
			Component idClassSource,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess,
			JavaType<I> idJavaType) {
		return new NonAggregatedIdentifierMappingImpl<>( entityPersister, virtualIdSource,
				rootTableName, rootTableKeyColumnNames, creationProcess,
				(mapping, virtualId) -> new IdClassEmbeddable<>( idClassSource, bootEntityDescriptor,
						mapping, entityPersister, rootTableName, rootTableKeyColumnNames,
						virtualId, creationProcess, idJavaType ) );
	}

	private NonAggregatedIdentifierMappingImpl(
			EntityPersister<E> entityPersister,
			Component virtualIdSource,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess,
			BiFunction<NonAggregatedIdentifierMappingImpl<E, I>, VirtualIdEmbeddable<E>, IdentifierValueMapper<I>> mapperFactory) {
		super( entityPersister, rootTableName, creationProcess );
		entityDescriptor = entityPersister;
		virtualIdEmbeddable = new VirtualIdEmbeddable<>( virtualIdSource, this, entityPersister,
				rootTableName, rootTableKeyColumnNames, creationProcess );
		identifierValueMapper = mapperFactory.apply( this, virtualIdEmbeddable );
		idClassEmbeddable = identifierValueMapper instanceof IdClassEmbeddable<I> idClass ? idClass : null;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected NonAggregatedIdentifierMappingImpl(NonAggregatedIdentifierMappingImpl<E, I> original) {
		super( original );
		entityDescriptor = original.entityDescriptor;
		virtualIdEmbeddable = original.virtualIdEmbeddable;
		idClassEmbeddable = original.idClassEmbeddable;
		identifierValueMapper = original.identifierValueMapper;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType<E, E, ?> getMappedType() {
		return virtualIdEmbeddable;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType<E, E, ?> getPartMappingType() {
		return getMappedType();
	}

	@Nullable
	@Override
	public IdClassEmbeddable<I> getIdClassEmbeddable() {
		return idClassEmbeddable;
	}

	@Nonnull
	@Override
	public VirtualIdEmbeddable<E> getVirtualIdEmbeddable() {
		return virtualIdEmbeddable;
	}

	@Nonnull
	@Override
	public IdentifierValueMapper<I> getIdentifierValueMapper() {
		return identifierValueMapper;
	}

	@Override
	public boolean hasContainingClass() {
		return idClassEmbeddable != null;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType<?, ?, ?> getMappedIdEmbeddableTypeDescriptor() {
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
			@Nullable X x, @Nullable Y y, @Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
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

	@Nonnull
	@Override
	public Nature getNature() {
		return Nature.VIRTUAL;
	}

	@Nullable
	@Override
	public String getAttributeName() {
		return null;
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
							castNonNull( idClassAttributeMapping.getPropertyAccess() ).getGetter().getReturnTypeClass().isPrimitive()
									? idClassAttributeMapping.getExpressibleJavaType().getDefaultValue()
									: null;
				}
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				else if ( attributeMapping instanceof ToOneAttributeMapping<?> toOneAttributeMapping
						&& !( identifierValueMapper.getAttributeMapping( i ) instanceof ToOneAttributeMapping<?> ) ) {
					final Object toOne = getIfMerged( o, mergeContext );
					final var targetPart =
							toOneAttributeMapping.getForeignKeyDescriptor()
									.getPart( toOneAttributeMapping.getSideNature().inverse() );
					propertyValues[i] =
							targetPart.isEntityIdentifierMapping()
									? ((EntityIdentifierMapping<?, ?, ?>) targetPart).getIdentifier( toOne, mergeContext )
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
		for ( int i = 0; i < propertyValues.length; i++ ) {
			final var attribute = embeddableTypeDescriptor.getAttributeMapping( i );
			final var mappedIdAttributeMapping = identifierValueMapper.getAttributeMapping( i );
			Object object = id == null ? mappedIdAttributeMapping.getExpressibleJavaType().getDefaultValue()
					: mappedIdAttributeMapping.getValue( id );
			if ( object != null && attribute instanceof ToOneAttributeMapping<?> toOneAttributeMapping
					&& !( mappedIdAttributeMapping instanceof ToOneAttributeMapping<?> ) ) {
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
			@Nonnull BiConsumer<SqlSelection, JdbcMapping<?>> selectionConsumer) {
		identifierValueMapper.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Nonnull
	@Override
	public DomainResult<I> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
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

	@Nonnull
	@Override
	public Fetchable<?, ?, ?> getFetchable(int position) {
		return getPartMappingType().getFetchable( position );
	}
}
