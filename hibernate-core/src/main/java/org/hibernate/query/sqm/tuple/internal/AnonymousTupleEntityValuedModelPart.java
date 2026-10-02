package org.hibernate.query.sqm.tuple.internal;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
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
import org.hibernate.metamodel.mapping.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.LazyTableGroup;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.annotation.Nullable;

import static java.util.Objects.requireNonNullElse;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Christian Beikov
 */
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

	@Nullable
	@Override
	public ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
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
	public void visitSubParts(@Nonnull Consumer<ModelPart> consumer, @Nullable EntityMappingType treatTargetType) {
		delegate.visitSubParts( consumer, treatTargetType );
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return getPartMappingType();
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Nonnull
	@Override
	public String getPartName() {
		return castNonNull( delegate.getPartName() );
	}

	@Nonnull
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

	@Nonnull
	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return delegate.getEntityMappingType().getAttributeMapping( position );
	}

	@Nonnull
	@Override
	public AttributeMappingsList getAttributeMappings() {
		return delegate.getEntityMappingType().getAttributeMappings();
	}

	@Override
	public void forEachAttributeMapping(@Nonnull Consumer<? super AttributeMapping> action) {
		delegate.getEntityMappingType().forEachAttributeMapping( action );
	}

	@Nonnull
	@Override
	public Object[] getValues(@Nonnull Object instance) {
		return delegate.getEntityMappingType().getValues( instance );
	}

	@Nullable
	@Override
	public Object getValue(@Nonnull Object instance, int position) {
		return delegate.getEntityMappingType().getValue( instance, position );
	}

	@Override
	public void setValues(@Nonnull Object instance, @Nonnull Object[] resolvedValues) {
		delegate.getEntityMappingType().setValues( instance, resolvedValues );
	}

	@Override
	public void setValue(@Nonnull Object instance, int position, @Nullable Object value) {
		delegate.getEntityMappingType().setValue( instance, position, value );
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return identifierMapping.getJdbcMapping( index );
	}

	@Nonnull
	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return identifierMapping.getSingleJdbcMapping();
	}

	@Override
	public int forEachSelectable(@Nonnull SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		return identifierMapping.forEachSelectable( offset, consumer );
	}

	@Nonnull
	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return identifierMapping.getSelectable( columnIndex );
	}

	@Nonnull
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
				lazyTableGroup
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

	@Nonnull
	@Override
	public String getSqlAliasStem() {
		return ((TableGroupJoinProducer) delegate).getSqlAliasStem();
	}

	@Override
	public int getNumberOfFetchables() {
		return delegate.getNumberOfFetchables();
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return castNonNull( delegate.getNavigableRole() );
	}

	@Nonnull
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return this;
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		return delegate.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		identifierMapping.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		identifierMapping.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return delegate.breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return delegate.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		delegate.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return delegate.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> consumer,
			@Nullable SharedSessionContractImplementor session) {
		return delegate.forEachJdbcValue( value, offset, x, y, consumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return delegate.forEachJdbcType( offset, action );
	}

	@Nonnull
	@Override
	public EntityPersister getEntityPersister() {
		return delegate.getEntityMappingType().getEntityPersister();
	}

	@Nonnull
	@Override
	public String getEntityName() {
		return delegate.getEntityMappingType().getEntityName();
	}

	@Nonnull
	@Override
	public TableDetails getMappedTableDetails() {
		return delegate.getEntityMappingType().getMappedTableDetails();
	}

	@Nonnull
	@Override
	public TableDetails getIdentifierTableDetails() {
		return delegate.getEntityMappingType().getIdentifierTableDetails();
	}

	@Override
	public void forEachTableDetails(@Nonnull Consumer<TableDetails> consumer) {
		delegate.getEntityMappingType().forEachTableDetails( consumer );
	}

	@Override
	public void visitQuerySpaces(@Nonnull Consumer<String> querySpaceConsumer) {
		delegate.getEntityMappingType().visitQuerySpaces( querySpaceConsumer );
	}

	@Nullable
	@Override
	public AttributeMapping findDeclaredAttributeMapping(@Nonnull String name) {
		return delegate.getEntityMappingType().findDeclaredAttributeMapping( name );
	}

	@Nonnull
	@Override
	public AttributeMappingsMap getDeclaredAttributeMappings() {
		return delegate.getEntityMappingType().getDeclaredAttributeMappings();
	}

	@Override
	public void visitDeclaredAttributeMappings(@Nonnull Consumer<? super AttributeMapping> action) {
		delegate.getEntityMappingType().visitDeclaredAttributeMappings( action );
	}

	@Nonnull
	@Override
	public EntityIdentifierMapping getIdentifierMapping() {
		return delegate.getEntityMappingType().getIdentifierMapping();
	}

	@Nullable
	@Override
	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return delegate.getEntityMappingType().getDiscriminatorMapping();
	}

	@Nullable
	@Override
	public DiscriminatorValue getDiscriminatorValue() {
		return delegate.getEntityMappingType().getDiscriminatorValue();
	}

	@Nullable
	@Override
	public String getDiscriminatorSQLValue() {
		return delegate.getEntityMappingType().getDiscriminatorSQLValue();
	}

	@Nullable
	@Override
	public EntityVersionMapping getVersionMapping() {
		return delegate.getEntityMappingType().getVersionMapping();
	}

	@Nonnull
	@Override
	public OptimisticLockStyle optimisticLockStyle() {
		return delegate.getEntityMappingType().optimisticLockStyle();
	}

	@Nullable
	@Override
	public NaturalIdMapping getNaturalIdMapping() {
		return delegate.getEntityMappingType().getNaturalIdMapping();
	}

	@Nullable
	@Override
	public EntityRowIdMapping getRowIdMapping() {
		return delegate.getEntityMappingType().getRowIdMapping();
	}

	@Nullable
	@Override
	public SoftDeleteMapping getSoftDeleteMapping() {
		return delegate.getEntityMappingType().getSoftDeleteMapping();
	}

	@Nonnull
	@Override
	public TableDetails getSoftDeleteTableDetails() {
		return delegate.getEntityMappingType().getSoftDeleteTableDetails();
	}

	@Override
	public void visitConstraintOrderedTables(@Nonnull ConstraintOrderedTableConsumer consumer) {
		delegate.getEntityMappingType().visitConstraintOrderedTables( consumer );
	}

	@Nullable
	@Override
	public Object loadByUniqueKey(@Nonnull String propertyName, @Nonnull Object uniqueKey, @Nonnull SharedSessionContractImplementor session) {
		return delegate.getEntityMappingType().loadByUniqueKey( propertyName, uniqueKey, session );
	}

	@Nonnull
	@Override
	public NaturalIdLoader<?> getNaturalIdLoader() {
		return delegate.getEntityMappingType().getNaturalIdLoader();
	}

	@Nonnull
	@Override
	public MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		return delegate.getEntityMappingType().getMultiNaturalIdLoader();
	}

	@Nonnull
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
	public boolean isAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers) {
		return false;
	}

	@Override
	public boolean isAffectedByEnabledFilters(@Nonnull LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		return false;
	}

	@Override
	public boolean isNotAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers) {
		return true;
	}
}
