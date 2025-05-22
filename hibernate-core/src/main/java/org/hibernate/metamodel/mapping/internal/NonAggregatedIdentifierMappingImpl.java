/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingFetch;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingResult;

import org.checkerframework.checker.nullness.qual.Nullable;

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
			final Component virtualIdSource = bootEntityDescriptor.getIdentifierMapper();
			final Component idClassSource = (Component) bootEntityDescriptor.getIdentifier();

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
			final SelectableMappings selectableMappings = getEmbeddableTypeDescriptor();
			final List<ColumnReference> columnReferences = CollectionHelper.arrayList( selectableMappings.getJdbcTypeCount() );
			final NavigablePath navigablePath = tableGroup.getNavigablePath()
					.append( getNavigableRole().getNavigableName() );
			final TableReference defaultTableReference = tableGroup.resolveTableReference(
					navigablePath,
					getContainingTableExpression()
			);
			identifierValueMapper.forEachSelectable(
					0,
					(columnIndex, selection) -> {
						final TableReference tableReference = defaultTableReference.resolveTableReference( selection.getContainingTableExpression() ) != null
								? defaultTableReference
								: tableGroup.resolveTableReference(
								navigablePath,
								selection.getContainingTableExpression()
						);
						final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
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
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entity );
			if ( lazyInitializer != null ) {
				return lazyInitializer.getInternalIdentifier();
			}
			final EmbeddableMappingType embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
			final Object[] propertyValues = new Object[embeddableTypeDescriptor.getNumberOfAttributeMappings()];
			for ( int i = 0; i < propertyValues.length; i++ ) {
				final AttributeMapping attributeMapping = embeddableTypeDescriptor.getAttributeMapping( i );
				final Object o = attributeMapping.getValue( entity );
				if ( o == null ) {
					final AttributeMapping idClassAttributeMapping = identifierValueMapper.getAttributeMapping( i );
					if ( idClassAttributeMapping.getPropertyAccess().getGetter().getReturnTypeClass().isPrimitive() ) {
						propertyValues[i] = idClassAttributeMapping.getExpressibleJavaType().getDefaultValue();
					}
					else {
						propertyValues[i] = null;
					}
				}
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				else if ( attributeMapping instanceof ToOneAttributeMapping
						&& !( identifierValueMapper.getAttributeMapping( i ) instanceof ToOneAttributeMapping ) ) {
					final Object toOne = getIfMerged( o, mergeContext );
					final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
					final ModelPart targetPart = toOneAttributeMapping.getForeignKeyDescriptor().getPart(
							toOneAttributeMapping.getSideNature().inverse()
					);
					if ( targetPart.isEntityIdentifierMapping() ) {
						propertyValues[i] = ( (EntityIdentifierMapping) targetPart ).getIdentifier( toOne, mergeContext );
					}
					else {
						propertyValues[i] = toOne;
					}
				}
				else {
					propertyValues[i] = o;
				}
			}
			return identifierValueMapper.getRepresentationStrategy().getInstantiator().instantiate(
					() -> propertyValues,
					sessionFactory
			);
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
		final EmbeddableMappingType embeddableTypeDescriptor = getEmbeddableTypeDescriptor();
		for ( int i = 0; i < propertyValues.length; i++ ) {
			final AttributeMapping attribute = embeddableTypeDescriptor.getAttributeMapping( i );
			final AttributeMapping mappedIdAttributeMapping = identifierValueMapper.getAttributeMapping( i );
			Object o = mappedIdAttributeMapping.getValue( id );
			if ( attribute instanceof ToOneAttributeMapping && !( mappedIdAttributeMapping instanceof ToOneAttributeMapping ) ) {
				final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attribute;
				final EntityPersister entityPersister = toOneAttributeMapping.getEntityMappingType().getEntityPersister();
				final EntityKey entityKey = session.generateEntityKey( o, entityPersister );
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
				// use the managed object i.e. proxy or initialized entity
				o = holder == null ? null : holder.getManagedObject();
				if ( o == null ) {
					// get the association out of the entity itself
					o = entityDescriptor.getPropertyValue(
							entity,
							toOneAttributeMapping.getAttributeName()
					);
					if ( o == null ) {
						if ( holder != null && holder.isEventuallyInitialized() ) {
							o = holder.getEntity();
						}
						else {
							o = session.internalLoad(
									entityPersister.getEntityName(),
									entityKey.getIdentifier(),
									true,
									true
							);
						}
					}
				}
			}
			propertyValues[i] = o;
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
