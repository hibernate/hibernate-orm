/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.boot.model.internal.SoftDeleteHelper;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.metamodel.mapping.ordering.OrderByFragmentTranslator;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.CollectionMutationTarget;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.internal.TableGroupJoinHelper;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.CollectionTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.OneToManyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.SelectEagerCollectionFetch;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.boot.model.internal.SoftDeleteHelper.createNonSoftDeletedRestriction;
import static org.hibernate.boot.model.internal.SoftDeleteHelper.resolveSoftDeleteMapping;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMappingImpl
		extends AbstractAttributeMapping
		implements PluralAttributeMapping, FetchProfileAffectee, FetchOptions {
	private static final Logger log = Logger.getLogger( PluralAttributeMappingImpl.class );

	/**
	 * Allows callback after creation of the attribute mapping.
	 *
	 * Support for the {@linkplain CollectionPersister collection},
	 * {@linkplain CollectionPart element} and {@linkplain CollectionPart index}
	 * descriptors
	 */
	public interface Aware {
		/**
		 * Injects the created attribute mapping
		 */
		void injectAttributeMapping(PluralAttributeMapping attributeMapping);
	}

	@SuppressWarnings("rawtypes")
	private final CollectionMappingType collectionMappingType;
	private final String referencedPropertyName;
	private final String mapKeyPropertyName;

	private final CollectionPart elementDescriptor;
	private final CollectionPart indexDescriptor;
	private final CollectionIdentifierDescriptor identifierDescriptor;
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;
	private final SoftDeleteMapping softDeleteMapping;
	private Boolean hasSoftDelete;

	private final String bidirectionalAttributeName;

	private final CollectionPersister collectionDescriptor;
	private final String separateCollectionTable;

	private final String sqlAliasStem;

	private final IndexMetadata indexMetadata;

	private ForeignKeyDescriptor fkDescriptor;

	private OrderByFragment orderByFragment;
	private OrderByFragment manyToManyOrderByFragment;

	public PluralAttributeMappingImpl(
			String attributeName,
			Collection bootDescriptor,
			PropertyAccess propertyAccess,
			AttributeMetadata attributeMetadata,
			CollectionMappingType<?> collectionMappingType,
			int stateArrayPosition,
			int fetchableIndex,
			CollectionPart elementDescriptor,
			CollectionPart indexDescriptor,
			CollectionIdentifierDescriptor identifierDescriptor,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor,
			MappingModelCreationProcess creationProcess) {
		super( attributeName, fetchableIndex, declaringType, attributeMetadata, stateArrayPosition, propertyAccess );
		this.collectionMappingType = collectionMappingType;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
		this.identifierDescriptor = identifierDescriptor;
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.collectionDescriptor = collectionDescriptor;
		this.referencedPropertyName = bootDescriptor.getReferencedPropertyName();

		if ( bootDescriptor instanceof Map ) {
			this.mapKeyPropertyName = ( (Map) bootDescriptor ).getMapKeyPropertyName();
		}
		else {
			this.mapKeyPropertyName = null;
		}

		this.bidirectionalAttributeName = StringHelper.subStringNullIfEmpty( bootDescriptor.getMappedByProperty(), '.');

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( attributeName );

		if ( bootDescriptor.isOneToMany() ) {
			separateCollectionTable = null;
		}
		else {
			separateCollectionTable = ( (Joinable) collectionDescriptor ).getTableName();
		}

		final int baseIndex;
		if ( bootDescriptor instanceof List ) {
			baseIndex = ( (List) bootDescriptor ).getBaseIndex();
		}
		else {
			baseIndex = -1;
		}

		indexMetadata = new IndexMetadata() {
			@Override
			public CollectionPart getIndexDescriptor() {
				return indexDescriptor;
			}

			@Override
			public int getListIndexBase() {
				return baseIndex;
			}

			@Override
			public String getIndexPropertyName() {
				return mapKeyPropertyName;
			}
		};

		softDeleteMapping = resolveSoftDeleteMapping( this, bootDescriptor, getSeparateCollectionTable(), creationProcess.getCreationContext().getDialect() );

		injectAttributeMapping( elementDescriptor, indexDescriptor, collectionDescriptor, this );
	}


	/**
	 * For Hibernate Reactive
	 */
	protected PluralAttributeMappingImpl(PluralAttributeMappingImpl original) {
		super( original );
		this.collectionMappingType = original.collectionMappingType;
		this.elementDescriptor = original.elementDescriptor;
		this.indexDescriptor = original.indexDescriptor;
		this.identifierDescriptor = original.identifierDescriptor;
		this.fetchTiming = original.fetchTiming;
		this.fetchStyle = original.fetchStyle;
		this.softDeleteMapping = original.softDeleteMapping;
		this.hasSoftDelete = original.hasSoftDelete;
		this.collectionDescriptor = original.collectionDescriptor;
		this.referencedPropertyName = original.referencedPropertyName;
		this.mapKeyPropertyName = original.mapKeyPropertyName;
		this.bidirectionalAttributeName = original.bidirectionalAttributeName;
		this.sqlAliasStem = original.sqlAliasStem;
		this.separateCollectionTable = original.separateCollectionTable;
		this.indexMetadata = original.indexMetadata;
		this.fkDescriptor = original.fkDescriptor;
		this.orderByFragment = original.orderByFragment;
		this.manyToManyOrderByFragment = original.manyToManyOrderByFragment;
		injectAttributeMapping( elementDescriptor, indexDescriptor, collectionDescriptor, this );
	}

	private static void injectAttributeMapping(
			CollectionPart elementDescriptor,
			CollectionPart indexDescriptor,
			CollectionPersister collectionDescriptor,
			PluralAttributeMapping mapping) {
		if ( collectionDescriptor instanceof Aware ) {
			( (Aware) collectionDescriptor ).injectAttributeMapping( mapping );
		}

		if ( elementDescriptor instanceof Aware ) {
			( (Aware) elementDescriptor ).injectAttributeMapping( mapping );
		}

		if ( indexDescriptor instanceof Aware ) {
			( (Aware) indexDescriptor ).injectAttributeMapping( mapping );
		}
	}

	@Override
	public boolean isBidirectionalAttributeName(NavigablePath fetchablePath, ToOneAttributeMapping modelPart) {
		if ( bidirectionalAttributeName == null ) {
			// If the FK-target of the to-one mapping is the same as the FK-target of this plural mapping,
			// then we say this is bidirectional, given that this is only invoked for model parts of the collection elements
			return fkDescriptor.getTargetPart() == modelPart.getForeignKeyDescriptor().getTargetPart();
		}
		return fetchablePath.getLocalName().endsWith( bidirectionalAttributeName );
	}

	@SuppressWarnings("unused")
	public void finishInitialization(
			Property bootProperty,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		final boolean hasOrder = bootDescriptor.getOrderBy() != null;
		final boolean hasManyToManyOrder = bootDescriptor.getManyToManyOrdering() != null;

		if ( hasOrder || hasManyToManyOrder ) {
			final TranslationContext context = collectionDescriptor::getFactory;

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
	public SoftDeleteMapping getSoftDeleteMapping() {
		return softDeleteMapping;
	}

	@Override
	public TableDetails getSoftDeleteTableDetails() {
		return ( (CollectionMutationTarget) getCollectionDescriptor() ).getCollectionTableMapping();
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
	public boolean containsTableReference(String tableExpression) {
		return tableExpression.equals( separateCollectionTable );
	}

	@Override
	public Generator getGenerator() {
		// can never be a generated value
		return null;
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
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public void applySoftDeleteRestrictions(TableGroup tableGroup, PredicateConsumer predicateConsumer) {
		if ( !hasSoftDelete() ) {
			// short-circuit
			return;
		}

		if ( getCollectionDescriptor().isOneToMany()
				|| getCollectionDescriptor().isManyToMany() ) {
			// see if the associated entity has soft-delete defined
			final EntityCollectionPart elementDescriptor = (EntityCollectionPart) getElementDescriptor();
			final EntityMappingType associatedEntityDescriptor = elementDescriptor.getAssociatedEntityMappingType();
			final SoftDeleteMapping softDeleteMapping = associatedEntityDescriptor.getSoftDeleteMapping();
			if ( softDeleteMapping != null ) {
				final Predicate softDeleteRestriction = SoftDeleteHelper.createNonSoftDeletedRestriction(
						tableGroup.resolveTableReference( associatedEntityDescriptor.getSoftDeleteTableDetails().getTableName() ),
						softDeleteMapping
				);
				predicateConsumer.applyPredicate( softDeleteRestriction );
			}
		}

		// apply the collection's soft-delete mapping, if one
		final SoftDeleteMapping softDeleteMapping = getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final Predicate softDeleteRestriction = SoftDeleteHelper.createNonSoftDeletedRestriction(
					tableGroup.resolveTableReference( getSoftDeleteTableDetails().getTableName() ),
					softDeleteMapping
			);
			predicateConsumer.applyPredicate( softDeleteRestriction );
		}
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

		// This is only used for collection initialization where we know the owner is available, so we mark it as visited
		// which will cause bidirectional to-one associations to be treated as such and avoid a join
		creationState.registerVisitedAssociationKey( fkDescriptor.getAssociationKey() );

		//noinspection unchecked
		return new CollectionDomainResult( navigablePath, this, resultVariable, tableGroup, creationState );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		final boolean added = creationState.registerVisitedAssociationKey( fkDescriptor.getAssociationKey() );

		try {
			if ( fetchTiming == FetchTiming.IMMEDIATE ) {
				if ( selected ) {
					final TableGroup collectionTableGroup = resolveCollectionTableGroup(
							fetchParent,
							fetchablePath,
							creationState,
							sqlAstCreationState
					);

					return buildEagerCollectionFetch(
							fetchablePath,
							this,
							collectionTableGroup,
							referencedPropertyName != null,
							fetchParent,
							creationState
					);
				}
				else {
					return createSelectEagerCollectionFetch(
							fetchParent,
							fetchablePath,
							creationState,
							sqlAstCreationState
					);
				}
			}

			if ( getCollectionDescriptor().getCollectionType().hasHolder() ) {
				return createSelectEagerCollectionFetch(
						fetchParent,
						fetchablePath,
						creationState,
						sqlAstCreationState
				);
			}

			return createDelayedCollectionFetch( fetchParent, fetchablePath, creationState, sqlAstCreationState );
		}
		finally {
			// This is only necessary because the association key is too general i.e. also matching FKs that other associations would match
			// and on top of this, we are not handling circular fetches for plural attributes yet
			if ( added ) {
				creationState.removeVisitedAssociationKey( fkDescriptor.getAssociationKey() );
			}
		}
	}

	/**
	 * For Hibernate Reactive
	 */
	protected Fetch buildDelayedCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent,
			DomainResult<?> collectionKeyResult,
			boolean unfetched) {
		return new DelayedCollectionFetch( fetchedPath, fetchedAttribute, fetchParent, collectionKeyResult, unfetched );
	}

	/**
	 * For Hibernate Reactive
	 */
	protected Fetch buildSelectEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			DomainResult<?> collectionKeyDomainResult,
			FetchParent fetchParent) {
		return new SelectEagerCollectionFetch( fetchedPath, fetchedAttribute, collectionKeyDomainResult, fetchParent );
	}

	/**
	 * For Hibernate Reactive
	 */
	protected Fetch buildEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			TableGroup collectionTableGroup,
			boolean needsCollectionKeyResult,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		return new EagerCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				collectionTableGroup,
				needsCollectionKeyResult,
				fetchParent,
				creationState
		);
	}

	@Override
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		if ( fetchTiming == FetchTiming.IMMEDIATE ) {
			final boolean alreadyVisited = creationState.isAssociationKeyVisited( fkDescriptor.getAssociationKey() );
			if ( alreadyVisited ) {
				return createSelectEagerCollectionFetch(
						fetchParent,
						fetchablePath,
						creationState,
						creationState.getSqlAstCreationState()
				);
			}
		}

		return null;
	}

	private Fetch createSelectEagerCollectionFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		final DomainResult<?> collectionKeyDomainResult;
		if ( referencedPropertyName != null ) {
			collectionKeyDomainResult = getKeyDescriptor().createTargetDomainResult(
					fetchablePath,
					sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() ),
					fetchParent,
					creationState
			);
		}
		else {
			collectionKeyDomainResult = null;
		}
		return buildSelectEagerCollectionFetch( fetchablePath, this, collectionKeyDomainResult, fetchParent );
	}

	private TableGroup resolveCollectionTableGroup(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		return fromClauseAccess.resolveTableGroup(
				fetchablePath,
				p -> {
					final TableGroup lhsTableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = createTableGroupJoin(
							fetchablePath,
							lhsTableGroup,
							null,
							null,
							SqlAstJoinType.LEFT,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					lhsTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);
	}

	private Fetch createDelayedCollectionFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		final DomainResult<?> collectionKeyDomainResult;
		// Lazy property. A null foreign key domain result will lead to
		// returning a domain result assembler that returns LazyPropertyInitializer.UNFETCHED_PROPERTY
		final EntityMappingType containingEntityMapping = findContainingEntityMapping();
		final boolean unfetched;
		if ( fetchParent.getReferencedModePart() == containingEntityMapping
				&& containingEntityMapping.getEntityPersister().getPropertyLaziness()[getStateArrayPosition()] ) {
			collectionKeyDomainResult = null;
			unfetched = true;
		}
		else {
			if ( referencedPropertyName != null ) {
				collectionKeyDomainResult = getKeyDescriptor().createTargetDomainResult(
						fetchablePath,
						sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() ),
						fetchParent,
						creationState
				);
			}
			else {
				collectionKeyDomainResult = null;
			}
			unfetched = false;
		}
		return buildDelayedCollectionFetch(
				fetchablePath,
				this,
				fetchParent,
				collectionKeyDomainResult,
				unfetched
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return fkDescriptor.isSimpleJoinPredicate( predicate );
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
		final PredicateCollector collectionPredicateCollector = new PredicateCollector();
		final TableGroup tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				addsPredicate,
				collectionPredicateCollector::applyPredicate,
				creationState
		);
		final PredicateCollector predicateCollector;
		if ( tableGroup.getNestedTableGroupJoins().isEmpty() ) {
			// No nested table group joins means that the predicate has to be pushed to the last join
			predicateCollector = new PredicateCollector();
		}
		else {
			predicateCollector = collectionPredicateCollector;
		}

		getCollectionDescriptor().applyBaseRestrictions(
				predicateCollector::applyPredicate,
				tableGroup,
				true,
				creationState.getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				creationState
		);

		getCollectionDescriptor().applyBaseManyToManyRestrictions(
				predicateCollector::applyPredicate,
				tableGroup,
				true,
				creationState.getLoadQueryInfluencers().getEnabledFilters(),
				null,
				creationState
		);

		applySoftDeleteRestriction(
				predicateCollector::applyPredicate,
				tableGroup,
				creationState
		);

		if ( fetched ) {
			if ( orderByFragment != null ) {
				creationState.applyOrdering( tableGroup, orderByFragment );
			}

			if ( manyToManyOrderByFragment != null ) {
				creationState.applyOrdering( tableGroup, manyToManyOrderByFragment );
			}
		}

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				determineSqlJoinType( lhs, requestedJoinType, fetched ),
				tableGroup,
				collectionPredicateCollector.getPredicate()
		);
		if ( predicateCollector != collectionPredicateCollector ) {
			final TableGroupJoin joinForPredicate = TableGroupJoinHelper.determineJoinForPredicateApply( tableGroupJoin );
			joinForPredicate.applyPredicate( predicateCollector.getPredicate() );
		}
		return tableGroupJoin;
	}

	private boolean hasSoftDelete() {
		// NOTE : this needs to be done lazily because the associated entity mapping (if one)
		// does not know its SoftDeleteMapping yet when this is created
		if ( hasSoftDelete == null ) {
			if ( softDeleteMapping != null ) {
				hasSoftDelete = true;
			}
			else {
				if ( getElementDescriptor() instanceof EntityCollectionPart ) {
					final EntityMappingType associatedEntityMapping = ( (EntityCollectionPart) getElementDescriptor() ).getAssociatedEntityMappingType();
					hasSoftDelete = associatedEntityMapping.getSoftDeleteMapping() != null;
				}
				else {
					hasSoftDelete = false;
				}
			}
		}

		return hasSoftDelete;
	}

	private void applySoftDeleteRestriction(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		if ( !hasSoftDelete() ) {
			// short circuit
			return;
		}

		if ( getElementDescriptor() instanceof EntityCollectionPart ) {
			final EntityMappingType entityMappingType = ( (EntityCollectionPart) getElementDescriptor() ).getAssociatedEntityMappingType();
			final SoftDeleteMapping softDeleteMapping = entityMappingType.getSoftDeleteMapping();
			if ( softDeleteMapping != null ) {
				final TableDetails softDeleteTable = entityMappingType.getSoftDeleteTableDetails();
				predicateConsumer.accept( createNonSoftDeletedRestriction(
						tableGroup.resolveTableReference( softDeleteTable.getTableName() ),
						softDeleteMapping,
						creationState.getSqlExpressionResolver()
				) );
			}
		}

		final SoftDeleteMapping softDeleteMapping = getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final TableDetails softDeleteTable = getSoftDeleteTableDetails();
			predicateConsumer.accept( createNonSoftDeletedRestriction(
					tableGroup.resolveTableReference( softDeleteTable.getTableName() ),
					softDeleteMapping,
					creationState.getSqlExpressionResolver()
			) );
		}
	}

	public SqlAstJoinType determineSqlJoinType(TableGroup lhs, @Nullable SqlAstJoinType requestedJoinType, boolean fetched) {
		if ( hasSoftDelete() ) {
			return SqlAstJoinType.LEFT;
		}

		if ( requestedJoinType == null ) {
			if ( fetched ) {
				return getDefaultSqlAstJoinType( lhs );
			}
			else {
				return SqlAstJoinType.INNER;
			}
		}
		else {
			return requestedJoinType;
		}
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				false,
				predicateConsumer,
				creationState
		);
	}

	private TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		final CollectionPersister collectionDescriptor = getCollectionDescriptor();
		final SqlAstJoinType joinType = determineSqlJoinType( lhs, requestedJoinType, fetched );
		final SqlAliasBase sqlAliasBase = creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		final TableGroup tableGroup;
		if ( collectionDescriptor.isOneToMany() ) {
			tableGroup = createOneToManyTableGroup(
					lhs.canUseInnerJoins() && joinType == SqlAstJoinType.INNER,
					joinType,
					navigablePath,
					fetched,
					addsPredicate,
					explicitSourceAlias,
					sqlAliasBase,
					creationState
			);
		}
		else {
			tableGroup = createCollectionTableGroup(
					lhs.canUseInnerJoins() && joinType == SqlAstJoinType.INNER,
					joinType,
					navigablePath,
					fetched,
					addsPredicate,
					explicitSourceAlias,
					sqlAliasBase,
					creationState
			);
		}

		if ( predicateConsumer != null ) {
			predicateConsumer.accept( getKeyDescriptor().generateJoinPredicate( lhs, tableGroup, creationState ) );
		}

		return tableGroup;
	}


	@Override
	public void setForeignKeyDescriptor(ForeignKeyDescriptor fkDescriptor) {
		this.fkDescriptor = fkDescriptor;
	}

	private TableGroup createOneToManyTableGroup(
			boolean canUseInnerJoins,
			SqlAstJoinType joinType,
			NavigablePath navigablePath,
			boolean fetched,
			boolean addsPredicate,
			String sourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstCreationState creationState) {
		final SqlAliasBase sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				sourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		final TableGroup elementTableGroup = ( (OneToManyCollectionPart) elementDescriptor ).createAssociatedTableGroup(
				canUseInnerJoins,
				navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
				fetched,
				sourceAlias,
				sqlAliasBase,
				creationState
		);
		final OneToManyTableGroup tableGroup = new OneToManyTableGroup(
				this,
				elementTableGroup,
				creationState.getCreationContext().getSessionFactory()
		);
		// For inner joins we never need join nesting
		final boolean nestedJoin = joinType != SqlAstJoinType.INNER
				// For outer joins we need nesting if there might be an on-condition that refers to the element table
				&& ( addsPredicate
				|| isAffectedByEnabledFilters( creationState.getLoadQueryInfluencers(), creationState.applyOnlyLoadByKeyFilters() )
				|| collectionDescriptor.hasWhereRestrictions() );

		if ( indexDescriptor instanceof TableGroupJoinProducer ) {
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) indexDescriptor ).createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					tableGroup,
					null,
					sqlAliasBase,
					joinType,
					fetched,
					false,
					creationState
			);
			tableGroup.registerIndexTableGroup( tableGroupJoin, nestedJoin );
		}

		return tableGroup;
	}

	private TableGroup createCollectionTableGroup(
			boolean canUseInnerJoins,
			SqlAstJoinType joinType,
			NavigablePath navigablePath,
			boolean fetched,
			boolean addsPredicate,
			String sourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstCreationState creationState) {
		assert !getCollectionDescriptor().isOneToMany();
		final SqlAliasBase sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				sourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		final String collectionTableName = ( (Joinable) collectionDescriptor ).getTableName();
		final TableReference collectionTableReference = new NamedTableReference(
				collectionTableName,
				sqlAliasBase.generateNewAlias(),
				true
		);

		final CollectionTableGroup tableGroup = new CollectionTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				collectionTableReference,
				true,
				sqlAliasBase,
				s -> false,
				null,
				creationState.getCreationContext().getSessionFactory()
		);
		// For inner joins we never need join nesting
		final boolean nestedJoin = joinType != SqlAstJoinType.INNER
				// For outer joins we need nesting if there might be an on-condition that refers to the element table
				&& ( addsPredicate
				|| isAffectedByEnabledFilters( creationState.getLoadQueryInfluencers(), creationState.applyOnlyLoadByKeyFilters() )
				|| collectionDescriptor.hasWhereRestrictions() );

		if ( elementDescriptor instanceof TableGroupJoinProducer ) {
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) elementDescriptor ).createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					tableGroup,
					null,
					sqlAliasBase,
					nestedJoin ? SqlAstJoinType.INNER : joinType,
					fetched,
					false,
					creationState
			);
			tableGroup.registerElementTableGroup( tableGroupJoin, nestedJoin );
		}

		if ( indexDescriptor instanceof TableGroupJoinProducer ) {
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) indexDescriptor ).createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					tableGroup,
					null,
					sqlAliasBase,
					nestedJoin ? SqlAstJoinType.INNER : joinType,
					fetched,
					false,
					creationState
			);
			tableGroup.registerIndexTableGroup( tableGroupJoin, nestedJoin );
		}

		return tableGroup;
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase, Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState) {
		if ( getCollectionDescriptor().isOneToMany() ) {
			return createOneToManyTableGroup(
					canUseInnerJoins,
					SqlAstJoinType.INNER,
					navigablePath,
					false,
					false,
					explicitSourceAlias,
					explicitSqlAliasBase,
					creationState
			);
		}
		else {
			return createCollectionTableGroup(
					canUseInnerJoins,
					SqlAstJoinType.INNER,
					navigablePath,
					false,
					false,
					explicitSourceAlias,
					explicitSqlAliasBase,
					creationState
			);
		}
	}

	@Override
	public int getBatchSize() {
		return getCollectionDescriptor().getBatchSize();
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		return getCollectionDescriptor().isAffectedByEnabledFilters( influencers, onlyApplyForLoadByKeyFilters );
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEntityGraph( influencers );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		if ( collectionDescriptor instanceof FetchProfileAffectee ) {
			( (FetchProfileAffectee) collectionDescriptor ).registerAffectingFetchProfile( fetchProfileName);
		}
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
		if ( elementDescriptor instanceof ModelPartContainer ) {
			final ModelPart subPart = ( (ModelPartContainer) elementDescriptor ).findSubPart( name, null );
			if ( subPart != null ) {
				return subPart;
			}
		}
		final CollectionPart.Nature nature = CollectionPart.Nature.fromName( name );
		if ( nature != null ) {
			switch ( nature ) {
				case ELEMENT:
					return elementDescriptor;
				case INDEX:
					return indexDescriptor;
				case ID:
					return identifierDescriptor;
			}
		}

		return null;
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		consumer.accept( 0, elementDescriptor );

		int position = 1;
		if ( indexDescriptor != null ) {
			consumer.accept( position++, indexDescriptor );
		}

		if ( identifierDescriptor != null ) {
			consumer.accept( position+1, identifierDescriptor );
		}
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
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( elementDescriptor );
		if ( indexDescriptor != null ) {
			consumer.accept( indexDescriptor );
		}
	}

	@Override
	public String getContainingTableExpression() {
		return getKeyDescriptor().getKeyTable();
	}

	@Override
	public int getJdbcTypeCount() {
		return 0;
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		throw new IndexOutOfBoundsException( index );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return null;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return 0;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return elementDescriptor.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		elementDescriptor.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return elementDescriptor.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public String toString() {
		return "PluralAttribute(" + getCollectionDescriptor().getRole() + ")";
	}
}
