/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SelectionMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.metamodel.mapping.ordering.OrderByFragmentTranslator;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.SelectEagerCollectionFetch;
import org.hibernate.type.EntityType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMappingImpl extends AbstractAttributeMapping
		implements PluralAttributeMapping, FetchOptions {
	private static final Logger log = Logger.getLogger( PluralAttributeMappingImpl.class );

	public interface Aware {
		void injectAttributeMapping(PluralAttributeMapping attributeMapping);
	}

	@SuppressWarnings("rawtypes")
	private final CollectionMappingType collectionMappingType;
	private final int stateArrayPosition;
	private final PropertyAccess propertyAccess;
	private final StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess;

	private final CollectionPart elementDescriptor;
	private final CollectionPart indexDescriptor;
	private final CollectionIdentifierDescriptor identifierDescriptor;
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;

	private final String bidirectionalAttributeName;
	private final Boolean isInverse;

	private final CollectionPersister collectionDescriptor;
	private final String separateCollectionTable;

	private final String sqlAliasStem;

	private final IndexMetadata indexMetadata;

	private ForeignKeyDescriptor fkDescriptor;
	private ForeignKeyDescriptor elementFkDescriptor;
	private ForeignKeyDescriptor indexFkDescriptor;

	private OrderByFragment orderByFragment;
	private OrderByFragment manyToManyOrderByFragment;

	@SuppressWarnings({"WeakerAccess", "rawtypes"})
	public PluralAttributeMappingImpl(
			String attributeName,
			Collection bootDescriptor,
			PropertyAccess propertyAccess,
			StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess,
			CollectionMappingType collectionMappingType,
			int stateArrayPosition,
			CollectionPart elementDescriptor,
			CollectionPart indexDescriptor,
			CollectionIdentifierDescriptor identifierDescriptor,
			FetchStrategy fetchStrategy,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor) {
		this(
				attributeName,
				bootDescriptor,
				propertyAccess,
				stateArrayContributorMetadataAccess,
				collectionMappingType,
				stateArrayPosition,
				elementDescriptor,
				indexDescriptor,
				identifierDescriptor,
				fetchStrategy.getTiming(),
				fetchStrategy.getStyle(),
				cascadeStyle,
				declaringType,
				collectionDescriptor
		);
	}

	@SuppressWarnings({"WeakerAccess", "rawtypes"})
	public PluralAttributeMappingImpl(
			String attributeName,
			Collection bootDescriptor,
			PropertyAccess propertyAccess,
			StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess,
			CollectionMappingType collectionMappingType,
			int stateArrayPosition,
			CollectionPart elementDescriptor,
			CollectionPart indexDescriptor,
			CollectionIdentifierDescriptor identifierDescriptor,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor) {
		super( attributeName, declaringType );
		this.propertyAccess = propertyAccess;
		this.stateArrayContributorMetadataAccess = stateArrayContributorMetadataAccess;
		this.collectionMappingType = collectionMappingType;
		this.stateArrayPosition = stateArrayPosition;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
		this.identifierDescriptor = identifierDescriptor;
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.collectionDescriptor = collectionDescriptor;

		this.bidirectionalAttributeName = StringHelper.subStringNullIfEmpty( bootDescriptor.getMappedByProperty(), '.');

		this.isInverse = bootDescriptor.isInverse();

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( attributeName );

		if ( bootDescriptor.isOneToMany() ) {
			separateCollectionTable = null;
		}
		else {
			separateCollectionTable = ( (Joinable) collectionDescriptor ).getTableName();
		}

		indexMetadata = new IndexMetadata() {
			final int baseIndex;

			{
				if ( bootDescriptor instanceof List ) {
					baseIndex = ( (List) bootDescriptor ).getBaseIndex();
				}
				else {
					baseIndex = -1;
				}
			}

			@Override
			public CollectionPart getIndexDescriptor() {
				return indexDescriptor;
			}

			@Override
			public int getListIndexBase() {
				return baseIndex;
			}
		};

		if ( collectionDescriptor instanceof Aware ) {
			( (Aware) collectionDescriptor ).injectAttributeMapping( this );
		}

		if ( elementDescriptor instanceof Aware ) {
			( (Aware) elementDescriptor ).injectAttributeMapping( this );
		}

		if ( indexDescriptor instanceof Aware ) {
			( (Aware) indexDescriptor ).injectAttributeMapping( this );
		}
	}

	@Override
	public boolean isBidirectionalAttributeName(NavigablePath fetchablePath) {
		if ( isInverse ) {
			return true;
		}
		if ( bidirectionalAttributeName == null ) {
			return false;
		}
		return fetchablePath.getFullPath().endsWith( bidirectionalAttributeName );
	}

	@SuppressWarnings("unused")
	public void finishInitialization(
			Property bootProperty,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		final Dialect dialect = creationProcess.getCreationContext()
				.getSessionFactory()
				.getJdbcServices()
				.getDialect();
		if ( collectionDescriptor.getElementType() instanceof EntityType ) {
			creationProcess.registerForeignKeyPostInitCallbacks(
					"To-many key - " + getNavigableRole(),
					() -> {
						elementFkDescriptor = createForeignKeyDescriptor(
								bootDescriptor.getElement(),
								(EntityType) collectionDescriptor.getElementType(),
								creationProcess,
								dialect
						);
						return true;
					}
			);
		}
		if ( collectionDescriptor.getIndexType() instanceof EntityType ) {
			creationProcess.registerForeignKeyPostInitCallbacks(
					"To-many index - " + getNavigableRole(),
					() -> {
						indexFkDescriptor = createForeignKeyDescriptor(
								( (IndexedCollection) bootDescriptor ).getIndex(),
								(EntityType) collectionDescriptor.getIndexType(),
								creationProcess,
								dialect
						);

						return true;
					}
			);
		}

		final boolean hasOrder = bootDescriptor.getOrderBy() != null;
		final boolean hasManyToManyOrder = bootDescriptor.getManyToManyOrdering() != null;

		if ( hasOrder || hasManyToManyOrder ) {
			final TranslationContext context = () -> collectionDescriptor.getFactory().getSessionFactoryOptions().getJpaCompliance();

			if ( hasOrder ) {
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Translating order-by fragment [%s] for collection role : %s",
							bootDescriptor.getOrderBy(),
							collectionDescriptor.getRole()
					);
				}
				orderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getOrderBy(),
						this,
						context
				);
			}

			if ( hasManyToManyOrder ) {
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Translating many-to-many order-by fragment [%s] for collection role : %s",
							bootDescriptor.getOrderBy(),
							collectionDescriptor.getRole()
					);
				}
				manyToManyOrderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getManyToManyOrdering(),
						this,
						context
				);
			}
		}
	}

	private ForeignKeyDescriptor createForeignKeyDescriptor(
			Value fkBootDescriptorSource,
			EntityType entityType,
			MappingModelCreationProcess creationProcess,
			Dialect dialect) {
		final EntityPersister associatedEntityDescriptor =  creationProcess.getEntityPersister( entityType.getAssociatedEntityName() );
		final ModelPart fkTargetPart = entityType.isReferenceToPrimaryKey()
				? associatedEntityDescriptor.getIdentifierMapping()
				: associatedEntityDescriptor.findSubPart( entityType.getRHSUniqueKeyPropertyName() );

		if ( fkTargetPart instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart basicFkTargetPart = (BasicValuedModelPart) fkTargetPart;
			final Joinable collectionDescriptorAsJoinable = (Joinable) collectionDescriptor;
			final SelectionMapping keySelectionMapping = SelectionMappingImpl.from(
					collectionDescriptorAsJoinable.getTableName(),
					fkBootDescriptorSource.getColumnIterator().next(),
					basicFkTargetPart.getJdbcMapping(),
					dialect,
					creationProcess.getSqmFunctionRegistry()
			);
			return new SimpleForeignKeyDescriptor(
					keySelectionMapping,
					basicFkTargetPart
			);
		}
		else if ( fkTargetPart instanceof EmbeddableValuedModelPart ) {
			return MappingModelCreationHelper.buildTargetingEmbeddableForeignKeyDescriptor(
					(EmbeddableValuedModelPart) fkTargetPart,
					fkBootDescriptorSource,
					dialect,
					creationProcess
			);
		}
		else {
			throw new NotYetImplementedFor6Exception(
					"Support for composite foreign keys not yet implemented : " + collectionDescriptor
							.getRole()
			);
		}
	}

	@Override
	public NavigableRole getNavigableRole() {
		return getCollectionDescriptor().getNavigableRole();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public CollectionMappingType getMappedType() {
		return collectionMappingType;
	}

	@Override
	public ForeignKeyDescriptor getKeyDescriptor() {
		return fkDescriptor;
	}

	@Override
	public CollectionPersister getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public CollectionPart getElementDescriptor() {
		return elementDescriptor;
	}

	@Override
	public CollectionPart getIndexDescriptor() {
		return indexDescriptor;
	}

	@Override
	public IndexMetadata getIndexMetadata() {
		return indexMetadata;
	}

	@Override
	public CollectionIdentifierDescriptor getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Override
	public OrderByFragment getOrderByFragment() {
		return orderByFragment;
	}

	@Override
	public OrderByFragment getManyToManyOrderByFragment() {
		return manyToManyOrderByFragment;
	}

	@Override
	public String getSeparateCollectionTable() {
		return separateCollectionTable;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public StateArrayContributorMetadataAccess getAttributeMetadataAccess() {
		return stateArrayContributorMetadataAccess;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return fetchStyle;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final TableGroup collectionTableGroup = creationState.getSqlAstCreationState()
				.getFromClauseAccess()
				.getTableGroup( navigablePath );

		assert collectionTableGroup != null;

		//noinspection unchecked
		return new CollectionDomainResult( navigablePath, this, resultVariable, tableGroup, creationState );
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
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		creationState.registerVisitedAssociationKey( fkDescriptor.getAssociationKey() );

		if ( fetchTiming == FetchTiming.IMMEDIATE) {
			if ( selected ) {
				final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
				final TableGroup collectionTableGroup = fromClauseAccess.resolveTableGroup(
						fetchablePath,
						p -> {
							final TableGroup lhsTableGroup = fromClauseAccess.getTableGroup(
									fetchParent.getNavigablePath() );
							final TableGroupJoin tableGroupJoin = createTableGroupJoin(
									fetchablePath,
									lhsTableGroup,
									null,
									SqlAstJoinType.LEFT,
									lockMode,
									creationState.getSqlAstCreationState()
							);
							return tableGroupJoin.getJoinedGroup();
						}
				);

				return new EagerCollectionFetch(
						fetchablePath,
						this,
						collectionTableGroup,
						fetchParent,
						creationState
				);
			}
			else {
				return new SelectEagerCollectionFetch( fetchablePath, this, fetchParent );
			}
		}

		if ( getCollectionDescriptor().getCollectionType().hasHolder() ) {
			return new SelectEagerCollectionFetch( fetchablePath, this, fetchParent );
		}

		return new DelayedCollectionFetch(
				fetchablePath,
				this,
				fetchParent
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final CollectionPersister collectionDescriptor = getCollectionDescriptor();
		if ( collectionDescriptor.isOneToMany() ) {
			return createOneToManyTableGroupJoin(
					navigablePath,
					lhs,
					explicitSourceAlias,
					sqlAstJoinType,
					lockMode,
					aliasBaseGenerator,
					sqlExpressionResolver,
					creationContext
			);
		}
		else {
			return createCollectionTableGroupJoin(
					navigablePath,
					lhs,
					explicitSourceAlias,
					sqlAstJoinType,
					lockMode,
					aliasBaseGenerator,
					sqlExpressionResolver,
					creationContext
			);
		}
	}

	public void setForeignKeyDescriptor(ForeignKeyDescriptor fkDescriptor) {
		this.fkDescriptor = fkDescriptor;
	}

	@SuppressWarnings("unused")
	private TableGroupJoin createOneToManyTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final TableGroup tableGroup = createOneToManyTableGroup(
				navigablePath,
				sqlAstJoinType == SqlAstJoinType.INNER
						&& !getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
				lockMode,
				aliasBaseGenerator,
				sqlExpressionResolver,
				creationContext
		);

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				tableGroup,
				getKeyDescriptor().generateJoinPredicate(
						lhs,
						tableGroup,
						sqlAstJoinType,
						sqlExpressionResolver,
						creationContext
				)
		);

		lhs.addTableGroupJoin( tableGroupJoin );

		return tableGroupJoin;
	}

	private TableGroup createOneToManyTableGroup(
			NavigablePath navigablePath,
			boolean canUseInnerJoins,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final EntityCollectionPart entityPartDescriptor;
		if ( elementDescriptor instanceof EntityCollectionPart ) {
			entityPartDescriptor = (EntityCollectionPart) elementDescriptor;
		}
		else {
			assert indexDescriptor instanceof EntityCollectionPart;
			entityPartDescriptor = (EntityCollectionPart) indexDescriptor;
		}

		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );

		final EntityMappingType entityMappingType = entityPartDescriptor.getEntityMappingType();
		final TableReference primaryTableReference = entityMappingType
				.createPrimaryTableReference(
						sqlAliasBase,
						sqlExpressionResolver,
						creationContext
				);

		return new StandardTableGroup(
				navigablePath,
				this,
				lockMode,
				primaryTableReference,
				sqlAliasBase,
				entityMappingType::containsTableReference,
				(tableExpression, tg) -> entityMappingType.createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						canUseInnerJoins,
						sqlExpressionResolver,
						creationContext
				),
				creationContext.getSessionFactory()
		);
	}

	@SuppressWarnings("unused")
	private TableGroupJoin createCollectionTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final TableGroup tableGroup = createCollectionTableGroup(
				navigablePath,
				sqlAstJoinType == SqlAstJoinType.INNER
						&& !getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
				lockMode,
				aliasBaseGenerator,
				sqlExpressionResolver,
				creationContext
		);

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				tableGroup,
				getKeyDescriptor().generateJoinPredicate(
						lhs,
						tableGroup,
						sqlAstJoinType,
						sqlExpressionResolver,
						creationContext
				)
		);

		lhs.addTableGroupJoin( tableGroupJoin );

		return tableGroupJoin;
	}

	private TableGroup createCollectionTableGroup(
			NavigablePath navigablePath,
			boolean canUseInnerJoin,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );

		assert !getCollectionDescriptor().isOneToMany();

		final String collectionTableName = ( (Joinable) collectionDescriptor ).getTableName();
		final TableReference collectionTableReference = new TableReference(
				collectionTableName,
				sqlAliasBase.generateNewAlias(),
				true,
				creationContext.getSessionFactory()
		);

		final EntityMappingType elementDescriptorEntityMappingType;
		if ( elementDescriptor instanceof EntityCollectionPart ) {
			elementDescriptorEntityMappingType = ( (EntityCollectionPart) elementDescriptor ).getEntityMappingType();
		}
		else {
			elementDescriptorEntityMappingType = null;
		}

		final EntityMappingType indexDescriptorEntityMappingType;
		if ( indexDescriptor instanceof EntityCollectionPart ) {
			indexDescriptorEntityMappingType = ( (EntityCollectionPart) indexDescriptor ).getEntityMappingType();
		}
		else {
			indexDescriptorEntityMappingType = null;
		}

		final BiFunction<String, TableGroup, TableReferenceJoin> tableReferenceJoinCreator;

		final java.util.function.Predicate<String> tableReferenceJoinNameChecker = createTableReferenceJoinNameChecker(
				elementDescriptorEntityMappingType,
				indexDescriptorEntityMappingType
		);

		final TableReference elementAssociatedPrimaryTable;
		final Function<TableGroup, TableReferenceJoin> elementTableGroupFinalizer;
		// todo (6.0) : not sure it is
		final boolean elementUseInnerJoin;
		if ( elementDescriptorEntityMappingType != null ) {
			elementUseInnerJoin = canUseInnerJoin && !getAttributeMetadataAccess()
					.resolveAttributeMetadata( elementDescriptorEntityMappingType ).isNullable();
			final SqlAstJoinType joinType = elementUseInnerJoin
					? SqlAstJoinType.INNER
					: SqlAstJoinType.LEFT;
			elementAssociatedPrimaryTable = elementDescriptorEntityMappingType.createPrimaryTableReference(
					sqlAliasBase,
					sqlExpressionResolver,
					creationContext
			);

			elementTableGroupFinalizer = createTableGroupFinalizer(
					sqlExpressionResolver,
					creationContext,
					collectionTableReference,
					elementAssociatedPrimaryTable,
					joinType,
					elementFkDescriptor
			);
		}
		else {
			elementAssociatedPrimaryTable = null;
			elementTableGroupFinalizer = null;
			elementUseInnerJoin = false;
		}

		TableReference indexAssociatedPrimaryTable;
		final Function<TableGroup, TableReferenceJoin> indexTableGroupFinalizer;
		final boolean indexUseInnerJoin;
		if ( indexDescriptorEntityMappingType != null ) {
			indexUseInnerJoin = canUseInnerJoin && !getAttributeMetadataAccess()
					.resolveAttributeMetadata( indexDescriptorEntityMappingType ).isNullable();
			final SqlAstJoinType joinType = indexUseInnerJoin
					? SqlAstJoinType.INNER
					: SqlAstJoinType.LEFT;
			indexAssociatedPrimaryTable = indexDescriptorEntityMappingType.createPrimaryTableReference(
					sqlAliasBase,
					sqlExpressionResolver,
					creationContext
			);

			indexTableGroupFinalizer = createTableGroupFinalizer(
					sqlExpressionResolver,
					creationContext,
					collectionTableReference,
					indexAssociatedPrimaryTable,
					joinType,
					indexFkDescriptor
			);
		}
		else {
			indexAssociatedPrimaryTable = null;
			indexTableGroupFinalizer = null;
			indexUseInnerJoin = false;
		}

		if ( elementDescriptorEntityMappingType != null || indexDescriptorEntityMappingType != null ) {
			tableReferenceJoinCreator = (tableExpression, tableGroup) -> {
				if ( elementDescriptorEntityMappingType != null
						&& elementDescriptorEntityMappingType.containsTableReference( tableExpression ) ) {
					return createTableReferenceJoin(
							sqlExpressionResolver,
							creationContext,
							sqlAliasBase,
							elementDescriptorEntityMappingType,
							elementAssociatedPrimaryTable,
							elementTableGroupFinalizer,
							elementUseInnerJoin,
							tableExpression,
							tableGroup
					);
				}
				else if ( indexDescriptorEntityMappingType != null
						&& indexDescriptorEntityMappingType.containsTableReference( tableExpression ) ) {
					return createTableReferenceJoin(
							sqlExpressionResolver,
							creationContext,
							sqlAliasBase,
							indexDescriptorEntityMappingType,
							indexAssociatedPrimaryTable,
							indexTableGroupFinalizer,
							indexUseInnerJoin,
							tableExpression,
							tableGroup
					);
				}
				throw new IllegalStateException( "could not create join for table `" + tableExpression + "`" );
			};
		}
		else {
			tableReferenceJoinCreator = (tableExpression, tableGroup) -> {
				throw new UnsupportedOperationException(
						"element-collection cannot contain joins : " + collectionTableReference.getTableExpression() + " -> " + tableExpression
				);
			};
		}

		final StandardTableGroup tableGroup = new StandardTableGroup(
				navigablePath,
				this,
				lockMode,
				collectionTableReference,
				sqlAliasBase,
				tableReferenceJoinNameChecker,
				tableReferenceJoinCreator,
				creationContext.getSessionFactory()
		);

		return tableGroup;
	}

	private TableReferenceJoin createTableReferenceJoin(
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext,
			SqlAliasBase sqlAliasBase,
			EntityMappingType elementDescriptorEntityMappingType,
			TableReference elementAssociatedPrimaryTable,
			Function<TableGroup, TableReferenceJoin> elementTableGroupFinalizer,
			boolean useInnerJoin,
			String tableExpression, TableGroup tableGroup) {
		if ( elementAssociatedPrimaryTable.getTableExpression().equals( tableExpression ) ) {
			TableReferenceJoin tableReferenceJoin = elementTableGroupFinalizer.apply( tableGroup );
			return tableReferenceJoin;
		}
		else {
			StandardTableGroup standardTableGroup = (StandardTableGroup) tableGroup;
			if ( standardTableGroup.getTableReferenceJoins().isEmpty() ) {
				TableReferenceJoin tableReferenceJoin = elementTableGroupFinalizer.apply( tableGroup );
				standardTableGroup.addTableReferenceJoin( tableReferenceJoin );
			}
		}
		return elementDescriptorEntityMappingType.createTableReferenceJoin(
				tableExpression,
				sqlAliasBase,
				elementAssociatedPrimaryTable,
				useInnerJoin,
				sqlExpressionResolver,
				creationContext
		);
	}

	private Function<TableGroup, TableReferenceJoin> createTableGroupFinalizer(
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext,
			TableReference collectionTableReference,
			TableReference elementAssociatedPrimaryTable,
			SqlAstJoinType joinType,
			ForeignKeyDescriptor elementFkDescriptor) {
		return tableGroup -> {

			final TableReferenceJoin associationJoin = new TableReferenceJoin(
					joinType,
					elementAssociatedPrimaryTable,
					elementFkDescriptor.generateJoinPredicate(
							collectionTableReference,
							elementAssociatedPrimaryTable,
							joinType,
							sqlExpressionResolver,
							creationContext
					)
			);
			return associationJoin;
		};
	}

	private java.util.function.Predicate<String> createTableReferenceJoinNameChecker(
			EntityMappingType elementDescriptorEntityMappingType,
			EntityMappingType indexDescriptorEntityMappingType) {
		return tableExpression -> {
			if ( elementDescriptorEntityMappingType != null
					&& elementDescriptorEntityMappingType.containsTableReference( tableExpression ) ) {
				return true;
			}
			if ( indexDescriptorEntityMappingType != null
					&& indexDescriptorEntityMappingType.containsTableReference( tableExpression ) ) {
				return true;
			}
			return false;
		};
	}

	@Override
	public TableGroup createRootTableGroup(
			NavigablePath navigablePath,
			String explicitSourceAlias,
			boolean canUseInnerJoins,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationContext creationContext) {
		if ( getCollectionDescriptor().isOneToMany() ) {
			return createOneToManyTableGroup(
					navigablePath,
					canUseInnerJoins,
					lockMode,
					aliasBaseGenerator,
					sqlExpressionResolver,
					creationContext
			);
		}
		else {
			return createCollectionTableGroup(
					navigablePath,
					canUseInnerJoins,
					lockMode,
					aliasBaseGenerator,
					sqlExpressionResolver,
					creationContext
			);
		}
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEnabledFilters( influencers );
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEntityGraph( influencers );
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEnabledFetchProfiles( influencers );
	}

	@Override
	public String getRootPathName() {
		return getCollectionDescriptor().getRole();
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromName( name );
		if ( nature == CollectionPart.Nature.ELEMENT ) {
			return elementDescriptor;
		}
		else if ( nature == CollectionPart.Nature.INDEX ) {
			return indexDescriptor;
		}
		else if ( nature == CollectionPart.Nature.ID ) {
			return identifierDescriptor;
		}

		if ( elementDescriptor instanceof EntityCollectionPart ) {
			return ( (EntityCollectionPart) elementDescriptor ).findSubPart( name );
		}
		else if ( elementDescriptor instanceof EmbeddedCollectionPart ) {
			return ( (EmbeddedCollectionPart) elementDescriptor ).findSubPart( name, treatTargetType );
		}
		return null;
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		elementDescriptor.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		elementDescriptor.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( elementDescriptor );
		if ( indexDescriptor != null ) {
			consumer.accept( indexDescriptor );
		}
	}

	@Override
	public int getJdbcTypeCount() {
		int span = elementDescriptor.getJdbcTypeCount();
		if ( indexDescriptor != null ) {
			span += indexDescriptor.getJdbcTypeCount();
		}
		return span;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = elementDescriptor.forEachJdbcType( offset, action );
		if ( indexDescriptor != null ) {
			span += indexDescriptor.forEachJdbcType( offset + span, action );
		}
		return span;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return elementDescriptor.disassemble( value,session );
	}

	@Override
	public int getNumberOfFetchables() {
		return indexDescriptor == null ? 1 : 2;
	}

	@Override
	public String toString() {
		return "PluralAttribute(" + getCollectionDescriptor().getRole() + ")";
	}
}
