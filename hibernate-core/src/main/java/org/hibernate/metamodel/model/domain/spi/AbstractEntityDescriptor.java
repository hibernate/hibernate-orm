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

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.MappedTableJoin;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterHelper;
import org.hibernate.loader.internal.StandardSingleIdEntityLoader;
import org.hibernate.loader.spi.EntityLocker;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.Representation;
import org.hibernate.metamodel.model.domain.internal.EntityIdentifierCompositeAggregatedImpl;
import org.hibernate.metamodel.model.domain.internal.EntityIdentifierSimpleImpl;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SqlAliasStemHelper;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfoSource;
import org.hibernate.sql.ast.produce.result.internal.QueryResultEntityImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroup;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroupEmptyImpl;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceGroupImpl;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.exec.results.internal.SqlSelectionGroupImpl;
import org.hibernate.sql.exec.results.spi.SqlSelectionGroup;
import org.hibernate.sql.exec.results.spi.SqlSelectionGroupEmpty;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityDescriptor<T>
		extends AbstractIdentifiableType<T>
		implements EntityDescriptor<T> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractEntityDescriptor.class );

	private final SessionFactoryImplementor factory;

	private final NavigableRole navigableRole;

	private final Table rootTable;
	private final List<JoinedTableBinding> secondaryTableBindings;

	private final Instantiator instantiator;
	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;

	private final String sqlAliasStem;

	@SuppressWarnings("UnnecessaryBoxing")
	public AbstractEntityDescriptor(
			EntityMapping bootMapping,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( resolveJavaTypeDescriptor( creationContext, bootMapping ) );

		this.factory = creationContext.getSessionFactory();

		this.navigableRole = new NavigableRole( bootMapping.getEntityName() );

		this.rootTable = resolveRootTable( bootMapping, creationContext );
		this.secondaryTableBindings = resolveSecondaryTableBindings( bootMapping, creationContext );

		Representation representation = bootMapping.getExplicitRepresentation();
		if ( representation == null ) {
			// todo (6.0) - if we move #defaultRepresentation into MetadataBuilder we can inject that into the entiyt mapping
			representation = creationContext.getSessionFactory()
					.getSessionFactoryOptions()
					.getDefaultRepresentation();
		}

		if ( representation == Representation.POJO ) {
			this.bytecodeEnhancementMetadata = BytecodeEnhancementMetadataPojoImpl.from( bootMapping );
		}
		else {
			this.bytecodeEnhancementMetadata = new BytecodeEnhancementMetadataNonPojoImpl( bootMapping.getEntityName() );
		}

		log.debugf(
				"Instantiated persister [%s] for entity [%s (%s)]",
				this,
				getJavaTypeDescriptor().getEntityName(),
				getJavaTypeDescriptor().getJpaEntityName()
		);

		if ( bootMapping.getExplicitInstantiator() != null ) {
			this.instantiator = bootMapping.getExplicitInstantiator();
		}
		else {
			// todo (6.0) - resolve ReflectionOptimizer to pass in to creating the instantiator
			this.instantiator = creationContext.getSessionFactory()
					.getSessionFactoryOptions()
					.getInstantiatorFactory()
					.createEntityInstantiator( bootMapping, this, null );
		}

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( getEntityName() );
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
				getPrimaryTable(),
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
			IdentifiableTypeDescriptor<? super T> superType,
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
	public EntityRegionAccessStrategy getCacheAccessStrategy() {
		return getHierarchy().getEntityRegionAccessStrategy();
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
	public EntityDescriptor<T> getEntityDescriptor() {
		return this;
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
		return getIdentifierDescriptor() instanceof EntityIdentifierSimpleImpl
				|| getIdentifierDescriptor() instanceof EntityIdentifierCompositeAggregatedImpl;
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
	public SingleIdEntityLoader getSingleIdLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( customQueryLoader != null ) {
			// if the user specified that we should use a custom query for loading this entity, we need
			// 		to always use that custom loader.
			return customQueryLoader;
		}


		if ( isAffectedByEnabledFilters( loadQueryInfluencers ) ) {
			// special case of not-cacheable based on enabled filters effecting this load.
			//
			// This case is special because the filters need to be applied in order to
			// 		properly restrict the SQL/JDBC results.  For this reason it has higher
			// 		precedence than even ""internal" fetch profiles.
			return createLoader( lockOptions, loadQueryInfluencers );
		}

		final boolean useInternalFetchProfile = loadQueryInfluencers.getEnabledInternalFetchProfileType() != null
				&& LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() );
		if ( useInternalFetchProfile ) {
			return internalCascadeLoaders.computeIfAbsent(
					loadQueryInfluencers.getEnabledInternalFetchProfileType(),
					internalFetchProfileType -> createLoader( lockOptions, loadQueryInfluencers )
			);
		}

		// otherwise see if the loader for the requested load can be cached (which
		// 		also means we should look in the cache).

		final boolean cacheable = ! isAffectedByEnabledFetchProfiles( loadQueryInfluencers )
				&& ! isAffectedByEntityGraph( loadQueryInfluencers )
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
			loader = createLoader( lockOptions, loadQueryInfluencers );
		}

		if ( cacheable ) {
			assert loaders != null;
			loaders.put( lockOptions.getLockMode(), loader );
		}

		return loader;
	}

	protected boolean isAffectedByEnabledFilters(LoadQueryInfluencers loadQueryInfluencers) {
		return loadQueryInfluencers.hasEnabledFilters()
				&& filterHelper.isAffectedBy( loadQueryInfluencers.getEnabledFilters() );
	}

	protected boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers loadQueryInfluencers) {
		for ( String s : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
			if ( affectingFetchProfileNames.contains( s ) ) {
				return true;
			}
		}
		return false;
	}

	protected boolean isAffectedByEntityGraph(LoadQueryInfluencers loadQueryInfluencers) {
		return loadQueryInfluencers.getFetchGraph() != null
				|| loadQueryInfluencers.getLoadGraph() != null;
	}

	protected SingleIdEntityLoader createLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		// todo (6.0) : determine when the loader can be cached....

		// for now, always create a new one
		return new StandardSingleIdEntityLoader<>(
				this,
				lockOptions,
				loadQueryInfluencers
		);
	}

	@Override
	public SingleUniqueKeyEntityLoader getSingleUniqueKeyLoader(Navigable navigable, LoadQueryInfluencers loadQueryInfluencers) {
		return null;
	}

	@Override
	public MultiIdEntityLoader getMultiIdLoader(LoadQueryInfluencers loadQueryInfluencers) {
		// todo (6.0) : disallow against entities for which the user has defined a custom "loader query".
		return null;
	}

	private Map<LockMode,EntityLocker> lockers;

	@Override
	public EntityLocker getLocker(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		EntityLocker entityLocker = null;
		if ( lockers == null ) {
			lockers = new ConcurrentHashMap<>();
		}
		else {
			entityLocker = lockers.get( lockOptions.getLockMode() );
		}

		if ( entityLocker == null ) {
			throw new NotYetImplementedException(  );
//			entityLocker = new EntityLocker() {
//				final LockingStrategy strategy = getFactory().getJdbcServices()
//						.getJdbcEnvironment()
//						.getDialect()
//						.getLockingStrategy( ... );
//				@Override
//				public void lock(
//						Serializable id,
//						Object version,
//						Object object,
//						SharedSessionContractImplementor session,
//						Options options) {
//					strategy.lock( id, version, object, options.getTimeout(), session );
//				}
//			};
//			lockers.put( lockOptions.getLockMode(), entityLocker );
		}
		return entityLocker;
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public EntityTableGroup createRootTableGroup(TableGroupInfoSource info, RootTableGroupContext tableGroupContext) {
		final SqlAliasBase sqlAliasBase = tableGroupContext.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		final TableReference primaryTableReference = resolvePrimaryTableReference( sqlAliasBase );

		final List<TableReferenceJoin> joins = new ArrayList<>(  );
		resolveTableReferenceJoins( primaryTableReference, sqlAliasBase, tableGroupContext, joins::add );

		final EntityTableGroup group = new EntityTableGroup(
				info.getUniqueIdentifier(),
				tableGroupContext.getTableSpace(),
				this,
				this,
				new NavigablePath( getEntityName() ),
				primaryTableReference,
				joins
		);

		// todo (6.0) - apply filters - which needs access to Session, or at least its LoadQueryInfluencers
		//		the filter conditions would be added to the SQL-AST's where-clause via tableGroupContext
		//		for now, add null, this is just here as a placeholder
		tableGroupContext.addRestriction( null );

		return group;
	}

	private TableReference resolvePrimaryTableReference(SqlAliasBase sqlAliasBase) {
		return new TableReference( getPrimaryTable(), sqlAliasBase.generateNewAlias() );
	}

	private void resolveTableReferenceJoins(
			TableReference rootTableReference,
			SqlAliasBase sqlAliasBase,
			TableGroupContext context,
			Consumer<TableReferenceJoin> collector) {
		getSecondaryTableBindings()
				.stream()
				.map( joinedTableBinding -> createTableReferenceJoin( joinedTableBinding, rootTableReference, sqlAliasBase, context ) )
				.forEach( collector );
	}

	private TableReferenceJoin createTableReferenceJoin(
			JoinedTableBinding joinedTableBinding,
			TableReference rootTableReference,
			SqlAliasBase sqlAliasBase,
			TableGroupContext context) {
		final TableReference joinedTableReference = new TableReference(
				joinedTableBinding.getTargetTable(),
				sqlAliasBase.generateNewAlias()
		);

		return new TableReferenceJoin(
				joinedTableBinding.isOptional()
						? JoinType.LEFT
						: context.getTableReferenceJoinType(),
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
							rootTableReference.resolveColumnReference( columnMapping.getTargetColumn() ),
							joinedTableReference.resolveColumnReference( columnMapping.getReferringColumn() )
					)
			);
		}

		return conjunction;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceSource lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {
		final TableReference root = resolvePrimaryTableReference( sqlAliasBase );
		joinCollector.addRoot( root );
		resolveTableReferenceJoins( root, sqlAliasBase, tableGroupContext, joinCollector::collectTableReferenceJoin );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultEntityImpl(
				(EntityReference) selectedExpression,
				resultVariable,
				buildSqlSelectionGroupMap( creationContext, selectedExpression ),
				selectedExpression.getNavigablePath(),
				creationContext
		);
	}

	private Map<PersistentAttribute, SqlSelectionGroup> buildSqlSelectionGroupMap(
			QueryResultCreationContext resolutionContext,
			NavigableReference selectedExpression) {
		final Map<PersistentAttribute, SqlSelectionGroup> sqlSelectionGroupMap = new HashMap<>();

		final LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap = buildColumnBindingGroupMap( selectedExpression, resolutionContext );
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

	private LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> buildColumnBindingGroupMap(
			NavigableReference selectedExpression,
			QueryResultCreationContext creationContext) {
		final LinkedHashMap<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap = new LinkedHashMap<>();

		// no matter what, include:
		//		1) identifier
		addColumnBindingGroupEntry( getHierarchy().getIdentifierDescriptor(), columnBindingGroupMap, creationContext );
		//		2) ROW_ID (if used)
		if ( getHierarchy().getRowIdDescriptor() != null ) {
			addColumnBindingGroupEntry( getHierarchy().getRowIdDescriptor(), columnBindingGroupMap, creationContext );
		}
		//		3) discriminator (if used)
		if ( getHierarchy().getDiscriminatorDescriptor() != null ) {
			addColumnBindingGroupEntry( getHierarchy().getDiscriminatorDescriptor(), columnBindingGroupMap, creationContext );
		}

		// Only render the rest of the attributes if !shallow
		if (  !creationContext.shouldCreateShallowEntityResult() ) {
			for ( PersistentAttribute<?,?> persistentAttribute : getPersistentAttributes() ) {
				addColumnBindingGroupEntry( persistentAttribute, columnBindingGroupMap, creationContext );
			}
		}

		return columnBindingGroupMap;
	}

	private void addColumnBindingGroupEntry(
			PersistentAttribute persistentAttribute,
			Map<PersistentAttribute, ColumnReferenceGroup> columnBindingGroupMap,
			QueryResultCreationContext creationContext) {
		if ( !SingularPersistentAttribute.class.isInstance( persistentAttribute ) ) {
			columnBindingGroupMap.put( persistentAttribute, ColumnReferenceGroupEmptyImpl.INSTANCE );
			return;
		}

		final SingularPersistentAttribute singularAttribute = (SingularPersistentAttribute) persistentAttribute;
		final ColumnReferenceGroupImpl columnBindingGroup = new ColumnReferenceGroupImpl();

		final List<Column> columns;
		if ( persistentAttribute instanceof SingularPersistentAttributeEmbedded ) {
			columns = ( (SingularPersistentAttributeEmbedded) singularAttribute ).getEmbeddedDescriptor().collectColumns();
		}
		else {
			columns = singularAttribute.getColumns();
		}

		for ( Column column : columns ) {
			columnBindingGroup.addColumnBinding( creationContext.currentColumnReferenceSource().resolveColumnReference( column ) );
		}

		columnBindingGroupMap.put( persistentAttribute, columnBindingGroup );
	}
}
