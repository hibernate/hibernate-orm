/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.OneToManyCollectionPart;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNullElse;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleEntityValuedModelPart
		implements EntityValuedModelPart, EntityMappingType, TableGroupJoinProducer, ValuedModelPart,
		LazyTableGroup.ParentTableGroupUseChecker {

	private final EntityIdentifierMapping identifierMapping;
	private final DomainType<?> domainType;
	private final EntityValuedModelPart delegate;
//	private final Set<String> targetKeyPropertyNames;
//	private final int fetchableIndex;

	public AnonymousTupleEntityValuedModelPart(
			EntityIdentifierMapping identifierMapping,
			DomainType<?> domainType,
			EntityValuedModelPart delegate,
			int fetchableIndex) {
		this.identifierMapping = identifierMapping;
		this.domainType = domainType;
		this.delegate = delegate;
		final EntityPersister persister = ((EntityMappingType) delegate.getPartMappingType())
				.getEntityPersister();
		final Set<String> targetKeyPropertyNames = new HashSet<>();
		targetKeyPropertyNames.add( EntityIdentifierMapping.ID_ROLE_NAME );
		ToOneAttributeMapping.addPrefixedPropertyNames(
				targetKeyPropertyNames,
				persister.getIdentifierPropertyName(),
				persister.getIdentifierType(),
				persister.getFactory()
		);
//		this.targetKeyPropertyNames = targetKeyPropertyNames;
//		this.fetchableIndex = fetchableIndex;
	}

	public ModelPart getForeignKeyPart() {
		// todo: naming?
		return identifierMapping;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			if ( identifierMapping.getAttributeName().equals( name ) ) {
				return identifierMapping;
			}
		}
		else {
			final ModelPart subPart = ( (CompositeIdentifierMapping) identifierMapping ).getPartMappingType().findSubPart(
					name,
					treatTargetType
			);
			if ( subPart != null ) {
				return subPart;
			}
		}
		return delegate.findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		delegate.visitSubParts( consumer, treatTargetType );
	}

	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Override
	public MappingType getMappedType() {
		return getPartMappingType();
	}

	@Override
	public JavaType<?> getJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Override
	public String getPartName() {
		return delegate.getPartName();
	}

	@Override
	public String getContainingTableExpression() {
		return "";
	}

	@Override
	public int getJdbcTypeCount() {
		return delegate.getJdbcTypeCount();
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return delegate.getEntityMappingType().getNumberOfAttributeMappings();
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return delegate.getEntityMappingType().getAttributeMapping( position );
	}

	@Override
	public AttributeMappingsList getAttributeMappings() {
		return delegate.getEntityMappingType().getAttributeMappings();
	}

	@Override
	public void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {
		delegate.getEntityMappingType().forEachAttributeMapping( action );
	}

	@Override
	public Object[] getValues(Object instance) {
		return delegate.getEntityMappingType().getValues( instance );
	}

	@Override
	public Object getValue(Object instance, int position) {
		return delegate.getEntityMappingType().getValue( instance, position );
	}

	@Override
	public void setValues(Object instance, Object[] resolvedValues) {
		delegate.getEntityMappingType().setValues( instance, resolvedValues );
	}

	@Override
	public void setValue(Object instance, int position, Object value) {
		delegate.getEntityMappingType().setValue( instance, position, value );
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return identifierMapping.getJdbcMapping( index );
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return identifierMapping.getSingleJdbcMapping();
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return identifierMapping.forEachSelectable( offset, consumer );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return identifierMapping.getSelectable( columnIndex );
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return delegate.getJavaType();
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final LazyTableGroup lazyTableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				null,
				creationState
		);
		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				joinType,
				lazyTableGroup,
				null
		);
		lazyTableGroup.setTableGroupInitializerCallback(
				createTableGroupInitializerCallback(
						lhs,
						tableGroupJoin::applyPredicate,
						creationState
				)
		);
		return tableGroupJoin;
	}

	private Consumer<TableGroup> createTableGroupInitializerCallback(
			TableGroup lhs,
			Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		// -----------------
		// Collect the selectable mappings for the FK key side and target side
		// As we will "resolve" the derived column references for these mappings
		// --------------

		final List<SelectableMapping> keyMappings;
		final List<SelectableMapping> targetMappings;

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();

		if ( delegate instanceof OneToManyCollectionPart oneToMany ) {
			final PluralAttributeMapping pluralAttribute = oneToMany.getCollectionDescriptor().getAttributeMapping();

			final ModelPart keyPart = pluralAttribute.getKeyDescriptor().getKeyPart();
			final ModelPart keyTargetPart = pluralAttribute.getKeyDescriptor().getTargetPart();

			keyMappings = arrayList( keyPart.getJdbcTypeCount() );
			keyPart.forEachSelectable( (selectionIndex, selectableMapping) -> keyMappings.add( selectableMapping ) );

			targetMappings = arrayList( keyTargetPart.getJdbcTypeCount() );
			keyTargetPart.forEachSelectable( (selectionIndex, selectableMapping) -> targetMappings.add( selectableMapping ) );
		}
		else {
			final EntityAssociationMapping associationMapping = (EntityAssociationMapping) delegate;

			if ( associationMapping.isReferenceToPrimaryKey() && associationMapping.getSideNature() == ForeignKeyDescriptor.Nature.KEY ) {
				final ModelPart targetJoinModelPart = associationMapping.getForeignKeyDescriptor()
						.getPart( associationMapping.getSideNature().inverse() );
				targetMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
				targetJoinModelPart.forEachSelectable(
						0,
						(i, selectableMapping) -> targetMappings.add( selectableMapping )
				);
				keyMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
				associationMapping.getForeignKeyDescriptor()
						.getPart( associationMapping.getSideNature() )
						.forEachSelectable(
								0,
								(i, selectableMapping) -> keyMappings.add( selectableMapping )
						);
			}
			else {
				final ModelPart targetJoinModelPart = delegate.getEntityMappingType().getIdentifierMapping();
				targetMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
				targetJoinModelPart.forEachSelectable(
						0,
						(i, selectableMapping) -> targetMappings.add( selectableMapping )
				);
				keyMappings = targetMappings;
			}
		}

		final TableReference tableReference = lhs.getPrimaryTableReference();
		final List<ColumnReference> keyColumnReferences = new ArrayList<>( this.identifierMapping.getJdbcTypeCount() );
		this.identifierMapping.forEachSelectable(
				(i, selectableMapping) -> {
					// It is important to resolve the sql expression here,
					// as this selectableMapping is the "derived" one.
					// We want to register the expression under the key of the original mapping
					// which leads to this expression being used for a possible domain result
					keyColumnReferences.add(
							(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey(
											tableReference,
											keyMappings.get( i ).getSelectionExpression(),
											keyMappings.get( i ).getJdbcMapping()
									),
									state -> new ColumnReference(
											tableReference,
											selectableMapping
									)
							)
					);
				}
		);
		if ( keyMappings != targetMappings ) {
			this.identifierMapping.forEachSelectable(
					(i, selectableMapping) -> {
						// It is important to resolve the sql expression here,
						// as this selectableMapping is the "derived" one.
						// We want to register the expression under the key of the original mapping
						// which leads to this expression being used for a possible domain result
						sqlExpressionResolver.resolveSqlExpression(
								SqlExpressionResolver.createColumnReferenceKey(
										tableReference,
										targetMappings.get( i ).getSelectionExpression(),
										targetMappings.get( i ).getJdbcMapping()
								),
								state -> new ColumnReference(
										tableReference,
										selectableMapping
								)
						);
					}
			);
		}
		return tg -> {
					this.identifierMapping.forEachSelectable(
							(i, selectableMapping) -> {
								final SelectableMapping targetMapping = targetMappings.get( i );
								final TableReference targetTableReference = tg.resolveTableReference(
										null,
										targetMapping.getContainingTableExpression()
								);
								predicateConsumer.accept(
										new ComparisonPredicate(
												keyColumnReferences.get( i ),
												ComparisonOperator.EQUAL,
												new ColumnReference(
														targetTableReference,
														targetMapping
												)
										)
								);
							}
					);
				};
	}

	public TableGroup createTableGroupInternal(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			String sourceAlias,
			final SqlAliasBase sqlAliasBase,
			SqlAstCreationState creationState) {
		final EntityMappingType entityMappingType = delegate.getEntityMappingType();
		final TableReference primaryTableReference = entityMappingType.createPrimaryTableReference(
				sqlAliasBase,
				creationState
		);

		return new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				primaryTableReference,
				true,
				sqlAliasBase,
				entityMappingType.getRootEntityDescriptor()::containsTableReference,
				(tableExpression, tg) -> entityMappingType.createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						creationState
				),
				creationState.getCreationContext().getSessionFactory()
		);
	}

	@Override
	public LazyTableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		final SqlAliasBase sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				explicitSourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		final boolean canUseInnerJoin = sqlAstJoinType == SqlAstJoinType.INNER || lhs.canUseInnerJoins();
		final LazyTableGroup lazyTableGroup = new LazyTableGroup(
				canUseInnerJoin,
				navigablePath,
				fetched,
				() -> createTableGroupInternal(
						canUseInnerJoin,
						navigablePath,
						fetched,
						null,
						sqlAliasBase,
						creationState
				),
				this,
				this,
				explicitSourceAlias,
				sqlAliasBase,
				creationState.getCreationContext().getSessionFactory(),
				lhs
		);

		if ( predicateConsumer != null ) {
			lazyTableGroup.setTableGroupInitializerCallback(
					createTableGroupInitializerCallback( lhs, predicateConsumer, creationState )
			);
		}

		return lazyTableGroup;
	}

	@Override
	public boolean canUseParentTableGroup(TableGroupProducer producer, NavigablePath navigablePath, ValuedModelPart valuedModelPart) {
		final ModelPart foreignKeyPart = getForeignKeyPart();
		if ( foreignKeyPart instanceof AnonymousTupleNonAggregatedEntityIdentifierMapping identifierMapping ) {
			final int numberOfFetchables = identifierMapping.getNumberOfFetchables();
			for ( int i = 0; i< numberOfFetchables; i++ ) {
				if ( valuedModelPart == identifierMapping.getFetchable( i ) ) {
					return true;
				}
			}
			return false;
		}
		return foreignKeyPart == valuedModelPart;
	}

	@Override
	public String getSqlAliasStem() {
		return ((TableGroupJoinProducer) delegate).getSqlAliasStem();
	}

	@Override
	public int getNumberOfFetchables() {
		return delegate.getNumberOfFetchables();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return delegate.getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return this;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return delegate.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		identifierMapping.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		identifierMapping.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return delegate.breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return delegate.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		delegate.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return delegate.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> consumer,
			SharedSessionContractImplementor session) {
		return delegate.forEachJdbcValue( value, offset, x, y, consumer, session );
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return false;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return delegate.forEachJdbcType( offset, action );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return delegate.getEntityMappingType().getEntityPersister();
	}

	@Override
	public String getEntityName() {
		return delegate.getEntityMappingType().getEntityName();
	}

	@Override
	public TableDetails getMappedTableDetails() {
		return delegate.getEntityMappingType().getMappedTableDetails();
	}

	@Override
	public TableDetails getIdentifierTableDetails() {
		return delegate.getEntityMappingType().getIdentifierTableDetails();
	}

	@Override
	public void visitQuerySpaces(Consumer<String> querySpaceConsumer) {
		delegate.getEntityMappingType().visitQuerySpaces( querySpaceConsumer );
	}

	@Override
	public AttributeMapping findDeclaredAttributeMapping(String name) {
		return delegate.getEntityMappingType().findDeclaredAttributeMapping( name );
	}

	@Override
	public AttributeMappingsMap getDeclaredAttributeMappings() {
		return delegate.getEntityMappingType().getDeclaredAttributeMappings();
	}

	@Override
	public void visitDeclaredAttributeMappings(Consumer<? super AttributeMapping> action) {
		delegate.getEntityMappingType().visitDeclaredAttributeMappings( action );
	}

	@Override
	public EntityIdentifierMapping getIdentifierMapping() {
		return delegate.getEntityMappingType().getIdentifierMapping();
	}

	@Override
	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return delegate.getEntityMappingType().getDiscriminatorMapping();
	}

	@Override
	public Object getDiscriminatorValue() {
		return delegate.getEntityMappingType().getDiscriminatorValue();
	}

	@Override
	public String getDiscriminatorSQLValue() {
		return delegate.getEntityMappingType().getDiscriminatorSQLValue();
	}

	@Override
	public EntityVersionMapping getVersionMapping() {
		return delegate.getEntityMappingType().getVersionMapping();
	}

	@Override
	public OptimisticLockStyle optimisticLockStyle() {
		return delegate.getEntityMappingType().optimisticLockStyle();
	}

	@Override
	public NaturalIdMapping getNaturalIdMapping() {
		return delegate.getEntityMappingType().getNaturalIdMapping();
	}

	@Override
	public EntityRowIdMapping getRowIdMapping() {
		return delegate.getEntityMappingType().getRowIdMapping();
	}

	@Override
	public SoftDeleteMapping getSoftDeleteMapping() {
		return delegate.getEntityMappingType().getSoftDeleteMapping();
	}

	@Override
	public TableDetails getSoftDeleteTableDetails() {
		return delegate.getEntityMappingType().getSoftDeleteTableDetails();
	}

	@Override
	public void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {
		delegate.getEntityMappingType().visitConstraintOrderedTables( consumer );
	}

	@Override
	public Object loadByUniqueKey(String propertyName, Object uniqueKey, SharedSessionContractImplementor session) {
		return delegate.getEntityMappingType().loadByUniqueKey( propertyName, uniqueKey, session );
	}

	@Override
	public NaturalIdLoader<?> getNaturalIdLoader() {
		return delegate.getEntityMappingType().getNaturalIdLoader();
	}

	@Override
	public MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		return delegate.getEntityMappingType().getMultiNaturalIdLoader();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return this;
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return delegate instanceof TableGroupJoinProducer
				? ( (TableGroupJoinProducer) delegate ).getDefaultSqlAstJoinType( parentTableGroup )
				: null;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return delegate instanceof TableGroupJoinProducer
			&& ( (TableGroupJoinProducer) delegate ).isSimpleJoinPredicate(predicate);
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return ( (TableGroupProducer) delegate ).containsTableReference( tableExpression );
	}

	@Override
	public int getBatchSize() {
		return -1;
	}

	@Override
	public boolean isAffectedByInfluencers(LoadQueryInfluencers influencers) {
		return false;
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		return false;
	}

	@Override
	public boolean isNotAffectedByInfluencers(LoadQueryInfluencers influencers) {
		return true;
	}
}
