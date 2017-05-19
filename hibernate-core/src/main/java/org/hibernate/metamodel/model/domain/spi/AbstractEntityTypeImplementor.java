/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.MappedTableJoin;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterHelper;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.domain.internal.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.internal.EntityIdentifierSimple;
import org.hibernate.loader.spi.EntityLocker;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableReferenceInfo;
import org.hibernate.sql.ast.produce.metamodel.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionGroupImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroup;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroupEmpty;
import org.hibernate.sql.ast.produce.result.internal.QueryResultEntityImpl;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroup;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.ast.tree.spi.select.EntityValuedSelectable;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityTypeImplementor<T>
		extends AbstractIdentifiableType<T>
		implements EntityTypeImplementor<T> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractEntityTypeImplementor.class );

	private final SessionFactoryImplementor factory;

	// needed temporarily between construction of the persister and its afterInitialization call
	private final EntityRegionAccessStrategy cacheAccessStrategy;
	private final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy;

	private final NavigableRole navigableRole;

	private final Table rootTable;
	private final List<JoinedTableBinding> secondaryTableBindings;

	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;
	private final EntityTuplizer tuplizer;

	@SuppressWarnings("UnnecessaryBoxing")
	public AbstractEntityTypeImplementor(
			EntityMapping entityMapping,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( resolveJavaTypeDescriptor( creationContext, entityMapping ) );

		this.factory = creationContext.getSessionFactory();
		this.cacheAccessStrategy = cacheAccessStrategy;
		this.naturalIdRegionAccessStrategy = naturalIdRegionAccessStrategy;

		this.navigableRole = new NavigableRole( entityMapping.getEntityName() );

		this.rootTable = resolveRootTable( entityMapping, creationContext );
		this.secondaryTableBindings = resolveSecondaryTableBindings( entityMapping, creationContext );

		if ( entityMapping.getEntityMode() == EntityMode.POJO ) {
			this.bytecodeEnhancementMetadata = BytecodeEnhancementMetadataPojoImpl.from( entityMapping );
		}
		else {
			this.bytecodeEnhancementMetadata = new BytecodeEnhancementMetadataNonPojoImpl( entityMapping.getEntityName() );
		}

		this.tuplizer = creationContext.getSessionFactory().getSessionFactoryOptions().getEntityTuplizerFactory().createTuplizer(
				entityMapping.getExplicitTuplizerClassName(),
				entityMapping.getEntityMode(),
				this,
				entityMapping,
				creationContext
		);

		log.debugf(
				"Instantiated persister [%s] for entity [%s (%s)]",
				this,
				getJavaTypeDescriptor().getEntityName(),
				getJavaTypeDescriptor().getJpaEntityName()
		);
	}

	// todo (6.0) : the root-table may not need to be phyically stored here
	// 		table structures vary by inheritance type
	//
	private Table resolveRootTable(EntityMapping entityMapping, RuntimeModelCreationContext creationContext) {
		final MappedTable rootMappedTable = entityMapping.getRootTable();
		return resolveTable( rootMappedTable, creationContext );
	}

	private Table resolveTable(MappedTable mappedTable, RuntimeModelCreationContext creationContext) {
		return creationContext.getDatabaseObjectResolver().resolveTable( mappedTable );
	}

	private List<JoinedTableBinding> resolveSecondaryTableBindings(
			EntityMapping entityMapping,
			RuntimeModelCreationContext creationContext) {
		final Collection<MappedTableJoin> secondaryTables = entityMapping.getSecondaryTables();
		if ( secondaryTables.size() <= 0 ) {
			return Collections.emptyList();
		}

		if ( secondaryTables.size() == 1 ) {
			return Collections.singletonList(
					generateJoinedTableBinding( secondaryTables.iterator().next(), creationContext )
			);
		}

		return secondaryTables.stream().map( m ->generateJoinedTableBinding( m, creationContext ) ).collect( Collectors.toList() );
	}

	private JoinedTableBinding generateJoinedTableBinding(MappedTableJoin mappedTableJoin, RuntimeModelCreationContext creationContext) {
		final Table joinedTable = resolveTable( mappedTableJoin.getMappedTable(), creationContext );

		// todo (6.0) : resolve "join predicate" as ForeignKey.ColumnMappings
		//		see note on MappedTableJoin regarding what to expose there

		return new JoinedTableBinding(
				joinedTable,
				getRootTable(),
				null,
				mappedTableJoin.isOptional()
		);
	}

	private static <T> IdentifiableJavaDescriptor<T> resolveJavaTypeDescriptor(
			RuntimeModelCreationContext creationContext,
			EntityMapping entityMapping) {
		return (EntityJavaDescriptor<T>) creationContext.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( entityMapping.getEntityName() );
	}

	@Override
	public void finishInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeImplementor<? super T> superType,
			EntityMapping mappingDescriptor,
			RuntimeModelCreationContext creationContext) {
		super.finishInitialization( entityHierarchy, superType, mappingDescriptor, creationContext );

		log.debugf(
				"Completed initialization of persister [%s] for entity [%s (%s)]",
				this,
				getJavaTypeDescriptor().getEntityName(),
				getJavaTypeDescriptor().getJpaEntityName()
		);
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public EntityJavaDescriptor<T> getJavaTypeDescriptor() {
		return (EntityJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public String getEntityName() {
		return getJavaTypeDescriptor().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getJavaTypeDescriptor().getJpaEntityName();
	}

	@Override
	public String getName() {
		return getJpaEntityName();
	}

	@Override
	public NavigableContainer getContainer() {
		return null;
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public Class<T> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	public EntityTuplizer getTuplizer() {
		return tuplizer;
	}

	@Override
	public EntityRegionAccessStrategy getCacheAccessStrategy() {
		return cacheAccessStrategy;
	}

	@Override
	public NaturalIdRegionAccessStrategy getNaturalIdCacheAccessStrategy() {
		return naturalIdRegionAccessStrategy;
	}

	@Override
	public BytecodeEnhancementMetadata getBytecodeEnhancementMetadata() {
		return bytecodeEnhancementMetadata;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getNavigableName() {
		return navigableRole.getNavigableName();
	}

	@Override
	public EntityTypeImplementor<T> getEntityDescriptor() {
		return this;
	}

	@Override
	public String getRolePrefix() {
		return getEntityName();
	}

	@Override
	public <Y> SingularAttribute<? super T, Y> getId(Class<Y> type) {
		return getHierarchy().getIdentifierDescriptor();
	}

	@Override
	public <Y> SingularAttribute<T, Y> getDeclaredId(Class<Y> type) {
		return getHierarchy().getIdentifierDescriptor();
	}

	@Override
	public <Y> SingularAttribute<? super T, Y> getVersion(Class<Y> type) {
		return getHierarchy().getVersionDescriptor();
	}

	@Override
	public <Y> SingularAttribute<T, Y> getDeclaredVersion(Class<Y> type) {
		return getHierarchy().getVersionDescriptor();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return getIdentifierDescriptor() instanceof EntityIdentifierSimple
				|| getIdentifierDescriptor() instanceof EntityIdentifierCompositeAggregated;
	}

	@Override
	public boolean hasVersionAttribute() {
		return getHierarchy().getVersionDescriptor() != null;
	}

	@Override
	public Set<SingularAttribute<? super T, ?>> getIdClassAttributes() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Type<?> getIdType() {
		return getHierarchy().getIdentifierDescriptor().getType();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	private final SingleIdEntityLoader customQueryLoader = null;
	private Map<LockMode,SingleIdEntityLoader> loaders;
	private Map<InternalFetchProfileType,SingleIdEntityLoader> internalCascadeLoaders;

	private final FilterHelper filterHelper = null;
	private final Set<String> affectingFetchProfileNames = new HashSet<>();


	@Override
	public SingleIdEntityLoader getSingleIdLoader(LockOptions lockOptions, SharedSessionContractImplementor session) {
		if ( customQueryLoader != null ) {
			// if the user specified that we should use a custom query for loading this entity, we need
			// 		to always use that custom loader.
			return customQueryLoader;
		}


		if ( isAffectedByEnabledFilters( session ) ) {
			// special case of not-cacheable based on enabled filters effecting this load.
			//
			// This case is special because the filters need to be applied in order to
			// 		properly restrict the SQL/JDBC results.  For this reason it has higher
			// 		precedence than even ""internal" fetch profiles.
			return createLoader( lockOptions, session );
		}

		final boolean useInternalFetchProfile = session.getLoadQueryInfluencers().getEnabledInternalFetchProfileType() != null
				&& LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() );
		if ( useInternalFetchProfile ) {
			return internalCascadeLoaders.computeIfAbsent(
					session.getLoadQueryInfluencers().getEnabledInternalFetchProfileType(),
					internalFetchProfileType -> createLoader( lockOptions, session )
			);
		}

		// otherwise see if the loader for the requested load can be cached (which
		// 		also means we should look in the cache).

		final boolean cacheable = ! isAffectedByEnabledFetchProfiles( session )
				&& ! isAffectedByEntityGraph( session )
				&& lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER;


		SingleIdEntityLoader loader = null;
		if ( cacheable ) {
			if ( loaders == null ) {
				loaders = new ConcurrentHashMap<>();
			}
			else {
				loader = loaders.get( lockOptions.getLockMode() );
			}
		}

		if ( loader == null ) {
			loader = createLoader( lockOptions, session );
		}

		if ( cacheable ) {
			assert loaders != null;
			loaders.put( lockOptions.getLockMode(), loader );
		}

		return loader;
	}

	protected boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
		return session.getLoadQueryInfluencers().hasEnabledFilters()
				&& filterHelper.isAffectedBy( session.getLoadQueryInfluencers().getEnabledFilters() );
	}

	protected boolean isAffectedByEnabledFetchProfiles(SharedSessionContractImplementor session) {
		for ( String s : session.getLoadQueryInfluencers().getEnabledFetchProfileNames() ) {
			if ( affectingFetchProfileNames.contains( s ) ) {
				return true;
			}
		}
		return false;
	}

	protected boolean isAffectedByEntityGraph(SharedSessionContractImplementor session) {
		return session.getLoadQueryInfluencers().getFetchGraph() != null
				|| session.getLoadQueryInfluencers().getLoadGraph() != null;
	}

	protected abstract SingleIdEntityLoader createLoader(LockOptions lockOptions, SharedSessionContractImplementor session);

	@Override
	public SingleUniqueKeyEntityLoader getSingleUniqueKeyLoader(Navigable navigable, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public MultiIdEntityLoader getMultiIdLoader(SharedSessionContractImplementor session) {
		// todo (6.0) : disallow against entities for which the user has defined a custom "loader query".
		return null;
	}

	@Override
	public EntityLocker getLocker(LockOptions lockOptions, SharedSessionContractImplementor session) {
		return null;
	}


	@Override
	public EntityTableGroup createRootTableGroup(
			NavigableReferenceInfo navigableReferenceInfo,
			RootTableGroupContext tableGroupContext,
			SqlAliasBaseResolver sqlAliasBaseResolver) {
		assert navigableReferenceInfo.getReferencedNavigable() instanceof NavigableEntityValued;
		assert ( (NavigableEntityValued) navigableReferenceInfo ).getEntityDescriptor() == this;
		// it is a root (supposedly)
		assert navigableReferenceInfo.getNavigableContainerReferenceInfo() == null;

		final EntityTableGroup group = createEntityTableGroup(
				navigableReferenceInfo,
				tableGroupContext,
				sqlAliasBaseResolver
		);

		// todo (6.0) - apply filters - which needs access to Session, or at least its LoadQueryInfluencers
		//		the filter conditions would be added to the SQL-AST's where-clause via tableGroupContext
		//		for now, add null, this is just here as a placeholder
		tableGroupContext.addRestriction( null );

		return group;
	}

	@Override
	public EntityTableGroup createEntityTableGroup(
			NavigableReferenceInfo navigableReferenceInfo,
			TableGroupContext tableGroupContext,
			SqlAliasBaseResolver sqlAliasBaseResolver) {
		final SqlAliasBase sqlAliasBase = sqlAliasBaseResolver.getSqlAliasBase( navigableReferenceInfo );

		final TableReference rootTableReference = resolveRootTableReference( sqlAliasBase );

		final List<TableReferenceJoin> joins = new ArrayList<>(  );
		resolveTableReferenceJoins( rootTableReference, sqlAliasBase, joins::add );

		return new EntityTableGroup(
				tableGroupContext.getTableSpace(),
				this,
				navigableReferenceInfo,
				// todo (6.0) :
				null,
				rootTableReference,
				joins
		);
	}

	private TableReference resolveRootTableReference(SqlAliasBase sqlAliasBase) {
		return new TableReference( getRootTable(), sqlAliasBase.generateNewAlias() );
	}

	private void resolveTableReferenceJoins(
			TableReference rootTableReference,
			SqlAliasBase sqlAliasBase,
			Consumer<TableReferenceJoin> collector) {
		getSecondaryTableBindings()
				.stream()
				.map( joinedTableBinding -> createTableReferenceJoin( joinedTableBinding, rootTableReference, sqlAliasBase ) )
				.forEach( collector );
	}

	private TableReferenceJoin createTableReferenceJoin(
			JoinedTableBinding joinedTableBinding,
			TableReference rootTableReference,
			SqlAliasBase sqlAliasBase) {
		final TableReference joinedTableReference = new TableReference(
				joinedTableBinding.getTargetTable(),
				sqlAliasBase.generateNewAlias()
		);

		return new TableReferenceJoin(
				joinedTableBinding.isOptional() ? SqmJoinType.LEFT : SqmJoinType.INNER,
				joinedTableReference,
				generateJoinPredicate( rootTableReference, joinedTableReference, joinedTableBinding.getJoinPredicateColumnMappings() )
		);
	}

	private Predicate generateJoinPredicate(
			TableReference rootTableReference,
			TableReference joinedTableReference,
			ForeignKey.ColumnMappings joinPredicateColumnMappings) {
		assert rootTableReference.getTable() == joinPredicateColumnMappings.getTargetTable();
		assert joinedTableReference.getTable() == joinPredicateColumnMappings.getReferringTable();
		assert !joinPredicateColumnMappings.getColumnMappings().isEmpty();

		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

		for ( ForeignKey.ColumnMapping columnMapping : joinPredicateColumnMappings.getColumnMappings() ) {
			conjunction.add(
					new RelationalPredicate(
							RelationalPredicate.Operator.EQUAL,
							new ColumnReference( columnMapping.getTargetColumn(), rootTableReference ),
							new ColumnReference( columnMapping.getReferringColumn(), joinedTableReference )
					)
			);
		}

		return conjunction;
	}

	@Override
	public void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector) {
		// todo (6.0) : NOTE the LHS is no longer passed in because we changed the expectation such that the caller would be the one creating the TableReference

		TableReference root = resolveRootTableReference( sqlAliasBase );
		collector.addRoot( root );
		resolveTableReferenceJoins( root, sqlAliasBase, collector::collectTableReferenceJoin );
	}


//	public TableGroupJoin applyTableGroupJoin(
//			NavigableReferenceInfo navigableReferenceInfo,
//			SqmJoinType joinType,
//			TableGroupJoinContext tableGroupJoinContext,
//			TableGroupResolver tableGroupResolutionContext,
//			SqlAliasBaseResolver sqlAliasBaseResolver) {
//		assert navigableReferenceInfo.getReferencedNavigable() instanceof EntityValuedNavigable;
//		assert navigableReferenceInfo.getReferencedNavigable() instanceof JoinablePersistentAttribute
//				|| navigableReferenceInfo.getReferencedNavigable() instanceof CollectionElement;
//		assert ( (EntityValuedNavigable) navigableReferenceInfo ).getEntityDescriptor() == this;
//		assert navigableReferenceInfo.getNavigableContainerReferenceInfo() != null;
//
//		final EntityTableGroup group = createEntityTableGroup(
//				navigableReferenceInfo,
//				tableGroupJoinContext.getTableSpace(),
//				sqlAliasBaseResolver
//		);
//
//		// todo (6.0) - apply filters - but which again needs access to Session, or at least its LoadQueryInfluencers
//		//		- see above note in #applyTableGroup
//		//		- here, though, we'd apply the predicate to the TableGroupJoin's predicate instead
//
//		// create the join predicate
//		final Predicate joinPredicate;
//		TableGroup lhsTableGroup = tableGroupResolutionContext.resolveTableGroup( navigableReferenceInfo.getNavigableContainerReferenceInfo().getUniqueIdentifier() );
//		final List<JoinColumnMapping> joinColumnMappings = ( (JoinablePersistentAttribute<?, ?>) navigableReferenceInfo.getReferencedNavigable() )
//				.getJoinColumnMappings();
//		if ( joinColumnMappings.size() == 1 ) {
//			joinPredicate = createJoinPredicate( joinColumnMappings.get( 0 ), lhsTableGroup, group );
//		}
//		else {
//			joinPredicate = new Junction( Junction.Nature.CONJUNCTION );
//			for ( JoinColumnMapping joinColumnMapping : joinColumnMappings ) {
//				( (Junction) joinPredicate ).add( createJoinPredicate( joinColumnMapping, lhsTableGroup, group ) );
//			}
//		}
//
//		// todo (6.0) : the null here is the join predicate - need to build it
//		//		however the
//		// todo (6.0) : create a "FilterableNavigable" or somesuch
//		TableGroupJoin tableGroupJoin = new TableGroupJoin(
//				joinType,
//				group,
//				joinPredicate
//		);
//
//		tableGroupJoinContext.getTableSpace().addJoinedTableGroup( tableGroupJoin );
//
//		return tableGroupJoin;
//
//	}
//
//	private Predicate createJoinPredicate(
//			JoinColumnMapping joinColumnMapping,
//			TableGroup lhsTableGroup,
//			EntityTableGroup rhsTableGroup) {
//		return new RelationalPredicate(
//				RelationalPredicate.Operator.EQUAL,
//				lhsTableGroup.resolveColumnReference( joinColumnMapping.getLeftHandSideColumn() ),
//				rhsTableGroup.resolveColumnReference( joinColumnMapping.getRightHandSideColumn() )
//		);
//	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultEntityImpl(
				selectedExpression,
				this,
				resultVariable,
				buildSqlSelectionGroupMap( creationContext, selectedExpression ),
				selectedExpression.getNavigablePath(),
				selectedExpression.getContributedColumnReferenceSource()
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceResolver,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		throw new NotYetImplementedException(  );
	}

	private Map<PersistentAttribute, SqlSelectionGroup> buildSqlSelectionGroupMap(
			QueryResultCreationContext resolutionContext,
			NavigableReference selectedExpression) {
		final Map<PersistentAttribute, SqlSelectionGroup> sqlSelectionGroupMap = new HashMap<>();

		final LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap =
				( (EntityValuedSelectable) selectedExpression.getSelectable() ).getColumnReferenceGroupMap();

		for ( Map.Entry<PersistentAttribute, ColumnReferenceGroup> entry : columnBindingGroupMap.entrySet() ) {
			sqlSelectionGroupMap.put(
					entry.getKey(),
					toSqlSelectionGroup( entry.getValue(), resolutionContext )
			);
		}

		return sqlSelectionGroupMap;
	}

	private SqlSelectionGroup toSqlSelectionGroup(ColumnReferenceGroup columnReferenceGroup, QueryResultCreationContext resolutionContext) {
		if ( columnReferenceGroup.getColumnReferences().isEmpty() ) {
			return SqlSelectionGroupEmpty.INSTANCE;
		}

		final SqlSelectionResolver sqlSelectionResolver = null;

		final SqlSelectionGroupImpl sqlSelectionGroup = new SqlSelectionGroupImpl();
		for ( ColumnReference columnReference : columnReferenceGroup.getColumnReferences() ) {
			sqlSelectionGroup.addSqlSelection( sqlSelectionResolver.resolveSqlSelection( columnReference ) );
		}
		return sqlSelectionGroup;
	}
}
