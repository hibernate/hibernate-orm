/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
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
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.jboss.logging.Logger;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMappingImpl extends AbstractAttributeMapping implements PluralAttributeMapping {
	private static final Logger log = Logger.getLogger( PluralAttributeMappingImpl.class );


	public interface Aware {
		void injectAttributeMapping(PluralAttributeMapping attributeMapping);
	}

	private final int stateArrayPosition;
	private final PropertyAccess propertyAccess;
	private final StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess;

	private final ForeignKeyDescriptor fkDescriptor;
	private final CollectionPart elementDescriptor;
	private final CollectionPart indexDescriptor;
	private final CollectionIdentifierDescriptor identifierDescriptor;

	private final FetchStrategy fetchStrategy;
	private final CascadeStyle cascadeStyle;

	private final CollectionPersister collectionDescriptor;
	private final String separateCollectionTable;

	private final String sqlAliasStem;

	private final IndexMetadata indexMetadata;

	private ForeignKeyDescriptor manyToManyFkDescriptor;

	private OrderByFragment orderByFragment;
	private OrderByFragment manyToManyOrderByFragment;

	@SuppressWarnings("WeakerAccess")
	public PluralAttributeMappingImpl(
			String attributeName,
			Collection bootDescriptor,
			PropertyAccess propertyAccess,
			StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess,
			CollectionMappingType collectionMappingType,
			int stateArrayPosition,
			ForeignKeyDescriptor fkDescriptor,
			CollectionPart elementDescriptor,
			CollectionPart indexDescriptor,
			CollectionIdentifierDescriptor identifierDescriptor,
			FetchStrategy fetchStrategy,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor) {
		super( attributeName, collectionMappingType, declaringType );
		this.propertyAccess = propertyAccess;
		this.stateArrayContributorMetadataAccess = stateArrayContributorMetadataAccess;
		this.stateArrayPosition = stateArrayPosition;
		this.fkDescriptor = fkDescriptor;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
		this.identifierDescriptor = identifierDescriptor;
		this.fetchStrategy = fetchStrategy;
		this.cascadeStyle = cascadeStyle;
		this.collectionDescriptor = collectionDescriptor;

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

	public void finishInitialization(
			Property bootProperty,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		if ( collectionDescriptor.getElementType() instanceof EntityType
				|| collectionDescriptor.getIndexType() instanceof EntityType ) {

			final EntityPersister associatedEntityDescriptor;
			final ModelPart fkTargetPart;
			final Value fkBootDescriptorSource;
			if ( collectionDescriptor.getElementType() instanceof EntityType ) {
				final EntityType elementEntityType = (EntityType) collectionDescriptor.getElementType();
				associatedEntityDescriptor = creationProcess.getEntityPersister( elementEntityType.getAssociatedEntityName() );
				fkTargetPart = elementEntityType.isReferenceToPrimaryKey()
						? associatedEntityDescriptor.getIdentifierMapping()
						: associatedEntityDescriptor.findSubPart( elementEntityType.getRHSUniqueKeyPropertyName() );
				fkBootDescriptorSource = bootDescriptor.getElement();
			}
			else {
				assert collectionDescriptor.getIndexType() != null;
				assert bootDescriptor instanceof IndexedCollection;

				final EntityType indexEntityType = (EntityType) collectionDescriptor.getIndexType();
				associatedEntityDescriptor = creationProcess.getEntityPersister( indexEntityType.getAssociatedEntityName() );
				fkTargetPart = indexEntityType.isReferenceToPrimaryKey()
						? associatedEntityDescriptor.getIdentifierMapping()
						: associatedEntityDescriptor.findSubPart( indexEntityType.getRHSUniqueKeyPropertyName() );
				fkBootDescriptorSource = ( (IndexedCollection) bootDescriptor ).getIndex();
			}

			if ( fkTargetPart instanceof BasicValuedModelPart ) {
				final BasicValuedModelPart basicFkTargetPart = (BasicValuedModelPart) fkTargetPart;
				final Joinable collectionDescriptorAsJoinable = (Joinable) collectionDescriptor;
				manyToManyFkDescriptor = new SimpleForeignKeyDescriptor(
						ForeignKeyDirection.TO_PARENT,
						collectionDescriptorAsJoinable.getTableName(),
						fkBootDescriptorSource.getColumnIterator().next().getText(),
						basicFkTargetPart.getContainingTableExpression(),
						basicFkTargetPart.getMappedColumnExpression(),
						basicFkTargetPart.getJdbcMapping()
				);
			}
			else {
				throw new NotYetImplementedFor6Exception(
						"Support for composite foreign keys not yet implemented : " + collectionDescriptor.getRole()
				);
			}
		}

		final boolean hasOrder = bootDescriptor.getOrderBy() != null;
		final boolean hasManyToManyOrder = bootDescriptor.getManyToManyOrdering() != null;

		if ( hasOrder || hasManyToManyOrder ) {
			final TranslationContext context = new TranslationContext() {
			};

			if ( hasOrder ) {
				log.debugf(
						"Translating order-by fragment [%s] for collection role : %s",
						bootDescriptor.getOrderBy(),
						collectionDescriptor.getRole()
				);
				orderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getOrderBy(),
						this,
						context
				);
			}

			if ( hasManyToManyOrder ) {
				log.debugf(
						"Translating many-to-many order-by fragment [%s] for collection role : %s",
						bootDescriptor.getOrderBy(),
						collectionDescriptor.getRole()
				);
				manyToManyOrderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getManyToManyOrdering(),
						this,
						context
				);
			}
		}
	}

	@Override
	public CollectionMappingType getMappedTypeDescriptor() {
		return (CollectionMappingType) super.getMappedTypeDescriptor();
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
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return getCollectionDescriptor().getNavigableRole();
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

		if ( fetchTiming == FetchTiming.IMMEDIATE || selected ) {
			final TableGroup collectionTableGroup = sqlAstCreationState.getFromClauseAccess().resolveTableGroup(
					fetchablePath,
					p -> {
						final TableGroup lhsTableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() );
						final TableGroupJoin tableGroupJoin = createTableGroupJoin(
								fetchablePath,
								lhsTableGroup,
								null,
								SqlAstJoinType.LEFT,
								lockMode,
								creationState.getSqlAliasBaseManager(),
								creationState.getSqlAstCreationState().getSqlExpressionResolver(),
								creationState.getSqlAstCreationState().getCreationContext()
						);

						lhsTableGroup.addTableGroupJoin( tableGroupJoin );

						sqlAstCreationState.getFromClauseAccess().registerTableGroup( fetchablePath, tableGroupJoin.getJoinedGroup() );

						return tableGroupJoin.getJoinedGroup();
					}
			);

			return new EagerCollectionFetch(
					fetchablePath,
					this,
					collectionTableGroup,
					getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
					fetchParent,
					creationState
			);
		}


		return new DelayedCollectionFetch(
				fetchablePath,
				this,
				true,
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
						&& ! getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
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

		final TableReference primaryTableReference = entityPartDescriptor.getEntityMappingType().createPrimaryTableReference(
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
				(tableExpression, tg) -> entityPartDescriptor.getEntityMappingType().createTableReferenceJoin(
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

		assert ! getCollectionDescriptor().isOneToMany();

		final String collectionTableName = ( (Joinable) collectionDescriptor ).getTableName();
		final TableReference collectionTableReference = new TableReference(
				collectionTableName,
				sqlAliasBase.generateNewAlias(),
				true,
				creationContext.getSessionFactory()
		);

		final Consumer<TableGroup> tableGroupFinalizer;
		final BiFunction<String,TableGroup,TableReferenceJoin> tableReferenceJoinCreator;
		if ( elementDescriptor instanceof EntityCollectionPart || indexDescriptor instanceof EntityCollectionPart ) {
			final EntityCollectionPart entityPartDescriptor;
			if ( elementDescriptor instanceof EntityCollectionPart ) {
				entityPartDescriptor = (EntityCollectionPart) elementDescriptor;
			}
			else {
				entityPartDescriptor = (EntityCollectionPart) indexDescriptor;
			}

			final EntityMappingType mappingType = entityPartDescriptor.getEntityMappingType();
			final TableReference associatedPrimaryTable = mappingType.createPrimaryTableReference(
					sqlAliasBase,
					sqlExpressionResolver,
					creationContext
			);

			tableReferenceJoinCreator = (tableExpression, tableGroup) -> mappingType.createTableReferenceJoin(
					tableExpression,
					sqlAliasBase,
					associatedPrimaryTable,
					canUseInnerJoin && ! getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
					sqlExpressionResolver,
					creationContext
			);

			tableGroupFinalizer = tableGroup -> {
				final SqlAstJoinType joinType = canUseInnerJoin && ! getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable()
						? SqlAstJoinType.INNER
						: SqlAstJoinType.LEFT;
				final TableReferenceJoin associationJoin = new TableReferenceJoin(
						joinType,
						associatedPrimaryTable,
						manyToManyFkDescriptor.generateJoinPredicate(
								collectionTableReference,
								associatedPrimaryTable,
								joinType,
								sqlExpressionResolver,
								creationContext
						)
				);
				( (StandardTableGroup) tableGroup ).addTableReferenceJoin( associationJoin );
			};
		}
		else {
			tableReferenceJoinCreator = (tableExpression, tableGroup) -> {
				throw new UnsupportedOperationException(
						"element-collection cannot contain joins : " + collectionTableReference.getTableExpression() + " -> " + tableExpression
				);
			};
			tableGroupFinalizer = null;
		}

		final StandardTableGroup tableGroup = new StandardTableGroup(
				navigablePath,
				this,
				lockMode,
				collectionTableReference,
				sqlAliasBase,
				tableReferenceJoinCreator,
				creationContext.getSessionFactory()
		);

		if ( tableGroupFinalizer != null ) {
			tableGroupFinalizer.accept( tableGroup );
		}

		return tableGroup;
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
			return ( (EntityCollectionPart) elementDescriptor ).findSubPart(name);
		}
		return null;
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( elementDescriptor );
		if ( indexDescriptor != null ) {
			consumer.accept( indexDescriptor );
		}
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
