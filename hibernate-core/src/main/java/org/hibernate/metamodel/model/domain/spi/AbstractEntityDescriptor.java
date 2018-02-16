/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedTableJoin;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterHelper;
import org.hibernate.loader.internal.StandardCompositeNaturalIdLoaderImpl;
import org.hibernate.loader.internal.StandardSimpleNaturalIdLoaderImpl;
import org.hibernate.loader.internal.StandardSingleIdEntityLoader;
import org.hibernate.loader.spi.EntityLocker;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.loader.spi.MultiIdLoaderSelectors;
import org.hibernate.loader.spi.NaturalIdLoader;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.internal.EntityHierarchyImpl;
import org.hibernate.metamodel.model.domain.internal.EntityIdentifierCompositeAggregatedImpl;
import org.hibernate.metamodel.model.domain.internal.EntityIdentifierSimpleImpl;
import org.hibernate.metamodel.model.domain.internal.SqlAliasStemHelper;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.results.internal.EntityQueryResultImpl;
import org.hibernate.sql.results.internal.EntitySqlSelectionMappingsImpl;
import org.hibernate.sql.results.spi.EntitySqlSelectionMappings;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityDescriptor<J>
		extends AbstractIdentifiableType<J>
		implements Lockable<J> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractEntityDescriptor.class );

	private final SessionFactoryImplementor factory;
	private final EntityHierarchy hierarchy;

	private final NavigableRole navigableRole;

	private final Table rootTable;
	private final List<JoinedTableBinding> secondaryTableBindings;

	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;
	private final Instantiator<J> instantiator;

	private final String sqlAliasStem;

	private final Dialect dialect;

	@SuppressWarnings("UnnecessaryBoxing")
	public AbstractEntityDescriptor(
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor<? super J> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super(
				bootMapping,
				superTypeDescriptor,
				resolveJavaTypeDescriptorFromJavaTypeMapping( bootMapping ),
				creationContext
		);

		this.factory = creationContext.getSessionFactory();

		this.navigableRole = new NavigableRole( bootMapping.getEntityName() );

		this.hierarchy = resolveEntityHierarchy( bootMapping, superTypeDescriptor, creationContext );

		this.rootTable = resolveRootTable( bootMapping, creationContext );
		this.secondaryTableBindings = resolveSecondaryTableBindings( bootMapping, creationContext );

		final RepresentationMode representation = getRepresentationStrategy().getMode();
		if ( representation == RepresentationMode.POJO ) {
			this.bytecodeEnhancementMetadata = BytecodeEnhancementMetadataPojoImpl.from( bootMapping );
		}
		else {
			this.bytecodeEnhancementMetadata = new BytecodeEnhancementMetadataNonPojoImpl( bootMapping.getEntityName() );
		}

		this.instantiator = getRepresentationStrategy().resolveInstantiator(
				bootMapping,
				this,
				Environment.getBytecodeProvider()
		);

		log.debugf(
				"Instantiated persister [%s] for entity [%s (%s)]",
				this,
				bootMapping.getEntityName(),
				bootMapping.getJpaEntityName()
		);

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( bootMapping.getEntityName() );
		this.dialect = factory.getServiceRegistry().getService( JdbcServices.class ).getDialect();
	}

	private EntityHierarchy resolveEntityHierarchy(
			IdentifiableTypeMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		if ( bootMapping instanceof RootClass ) {
			return new EntityHierarchyImpl( this, (RootClass) bootMapping, creationContext );
		}
		else {
			return superTypeDescriptor.getHierarchy();
		}
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

		final ArrayList<JoinedTableBinding> bindings = new ArrayList<>();
		for ( MappedTableJoin secondaryTable : secondaryTables ) {
			bindings.add(
					generateJoinedTableBinding( secondaryTable, creationContext )
			);
		}
		return bindings;
	}

	private JoinedTableBinding generateJoinedTableBinding(MappedTableJoin bootJoinTable, RuntimeModelCreationContext creationContext) {
		final Table joinedTable = resolveTable( bootJoinTable.getMappedTable(), creationContext );

		// todo (6.0) : resolve "join predicate" as ForeignKey.ColumnMappings
		//		see note on MappedTableJoin regarding what to expose there


		return new JoinedTableBinding(
				// NOTE : for secondary tables, it is the secondary table that is
				//		the referring table
				joinedTable,
				getPrimaryTable(),
				creationContext.getDatabaseObjectResolver().resolveForeignKey( bootJoinTable.getJoinMapping() ),
				bootJoinTable.isOptional()
		);
	}

	private static <T> IdentifiableJavaDescriptor<T> resolveJavaTypeDescriptorFromJavaTypeMapping(
			EntityMapping entityMapping) {
		return (IdentifiableJavaDescriptor<T>) entityMapping.getJavaTypeMapping().resolveJavaTypeDescriptor();
	}

	@Override
	public void finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		super.finishInitialization( bootDescriptor, creationContext );

		log.debugf(
				"Completed initialization of descriptor [%s] for entity [%s (%s)]",
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
	public EntityHierarchy getHierarchy() {
		return hierarchy;
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return (EntityJavaDescriptor<J>) super.getJavaTypeDescriptor();
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
	public Table getPrimaryTable() {
		return rootTable;
	}

	@Override
	public List<JoinedTableBinding> getSecondaryTableBindings() {
		return secondaryTableBindings;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
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
	public EntityDescriptor<J> getEntityDescriptor() {
		return this;
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		return getHierarchy().getMutabilityPlan().getEntityEntryFactory();
	}

	@Override
	public List<EntityNameResolver> getEntityNameResolvers() {
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularAttribute<? super J, Y> getId(Class<Y> type) {
		return (SingularAttribute<? super J, Y>) getHierarchy().getIdentifierDescriptor().asAttribute( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> SingularAttribute<J, Y> getDeclaredId(Class<Y> type) {
		return (SingularAttribute<J, Y>) getHierarchy().getIdentifierDescriptor().asAttribute( type );
	}

	@Override
	public <Y> SingularAttribute<? super J, Y> getVersion(Class<Y> type) {
		return getHierarchy().getVersionDescriptor();
	}

	@Override
	public <Y> SingularAttribute<J, Y> getDeclaredVersion(Class<Y> type) {
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
	public Set<SingularAttribute<? super J, ?>> getIdClassAttributes() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public Type<?> getIdType() {
		return getHierarchy().getIdentifierDescriptor();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	private final SingleIdEntityLoader customQueryLoader = null;
	private EnumMap<LockMode,SingleIdEntityLoader> loaders;
	private EnumMap<InternalFetchProfileType,SingleIdEntityLoader> internalCascadeLoaders;

	private final FilterHelper filterHelper = null;
	private final Set<String> affectingFetchProfileNames = new HashSet<>();

	@Override
	@SuppressWarnings("unchecked")
	public SingleIdEntityLoader getSingleIdLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( 	customQueryLoader != null ) {
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
			return createSingleIdLoader( lockOptions, loadQueryInfluencers );
		}

		if ( loadQueryInfluencers.getEnabledInternalFetchProfileType() != null ) {
			if ( LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() ) ) {
				if ( internalCascadeLoaders == null ) {
					internalCascadeLoaders = new EnumMap<>( InternalFetchProfileType.class );
				}
				return internalCascadeLoaders.computeIfAbsent(
						loadQueryInfluencers.getEnabledInternalFetchProfileType(),
						internalFetchProfileType -> createSingleIdLoader( lockOptions, loadQueryInfluencers )
				);
			}
		}

		// otherwise see if the loader for the requested load can be cached - which
		// 		also means we should look in the cache for an existing one

		final boolean cacheable = determineIfCacheable( lockOptions, loadQueryInfluencers );

		SingleIdEntityLoader loader = null;
		if ( cacheable ) {
			if ( loaders == null ) {
				loaders = new EnumMap<>( LockMode.class );
			}
			else {
				loader = loaders.get( lockOptions.getLockMode() );
			}
		}

		if ( loader == null ) {
			loader = createSingleIdLoader( lockOptions, loadQueryInfluencers );
		}

		if ( cacheable ) {
			assert loaders != null;
			loaders.put( lockOptions.getLockMode(), loader );
		}

		return loader;
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean determineIfCacheable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( isAffectedByEnabledFetchProfiles( loadQueryInfluencers ) ) {
			return false;
		}

		if ( isAffectedByEntityGraph( loadQueryInfluencers ) ) {
			return false;
		}

		if ( lockOptions.getTimeOut() == LockOptions.WAIT_FOREVER ) {
			return false;
		}

		return true;
	}

	private boolean isAffectedByEnabledFilters(LoadQueryInfluencers loadQueryInfluencers) {
		assert filterHelper != null;
		return loadQueryInfluencers.hasEnabledFilters()
				&& filterHelper.isAffectedBy( loadQueryInfluencers.getEnabledFilters() );
	}

	private boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers loadQueryInfluencers) {
		for ( String s : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
			if ( affectingFetchProfileNames.contains( s ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean isAffectedByEntityGraph(LoadQueryInfluencers loadQueryInfluencers) {
		return loadQueryInfluencers.getFetchGraph() != null
				|| loadQueryInfluencers.getLoadGraph() != null;
	}

	@SuppressWarnings("WeakerAccess")
	protected SingleIdEntityLoader createSingleIdLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		// todo (6.0) : determine when the loader can be cached....

		// for now, always create a new one
		return new StandardSingleIdEntityLoader<>(
				this,
				lockOptions,
				loadQueryInfluencers
		);
	}

	@Override
	public NaturalIdLoader getNaturalIdLoader(LockOptions lockOptions) {
		if ( getHierarchy().getNaturalIdDescriptor().getPersistentAttributes().size() > 1 ) {
			return new StandardCompositeNaturalIdLoaderImpl( this );
		}
		else {
			return new StandardSimpleNaturalIdLoaderImpl( this );
		}
	}


	private MultiIdEntityLoader orderedMultiIdLoader;
	private MultiIdEntityLoader unorderedMultiIdLoader;

	@Override
	public MultiIdEntityLoader getMultiIdLoader(MultiIdLoaderSelectors selectors) {
		if ( customQueryLoader != null ) {
			throw new HibernateException(
					"Cannot perform multi-id loading on an entity defined with a custom load query : " + getEntityName()
			);
		}

		if ( selectors.isOrderReturnEnabled() ) {
			if ( orderedMultiIdLoader == null ) {
				orderedMultiIdLoader = createOrderedMultiIdLoader();
			}
			return orderedMultiIdLoader;
		}


		if ( unorderedMultiIdLoader == null ) {
			unorderedMultiIdLoader = createUnorderedMultiIdLoader();
		}
		return unorderedMultiIdLoader;
	}

	private MultiIdEntityLoader createOrderedMultiIdLoader() {
		throw new NotYetImplementedFor6Exception();
	}

	private MultiIdEntityLoader createUnorderedMultiIdLoader() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SingleUniqueKeyEntityLoader getSingleUniqueKeyLoader(Navigable navigable, LoadQueryInfluencers loadQueryInfluencers) {
		throw new NotYetImplementedFor6Exception();
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
			throw new NotYetImplementedFor6Exception(  );
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
	public EntityTableGroup createRootTableGroup(TableGroupInfo info, RootTableGroupContext tableGroupContext) {
		final SqlAliasBase sqlAliasBase = tableGroupContext.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		final TableReference primaryTableReference = resolvePrimaryTableReference( sqlAliasBase );

		final List<TableReferenceJoin> joins = new ArrayList<>(  );
		resolveTableReferenceJoins( primaryTableReference, sqlAliasBase, tableGroupContext, joins::add );

		final EntityTableGroup group = new EntityTableGroup(
				info.getUniqueIdentifier(),
				tableGroupContext.getTableSpace(),
				this,
				tableGroupContext.getQueryOptions().getLockOptions().getEffectiveLockMode( info.getIdentificationVariable() ),
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

	protected TableReference resolvePrimaryTableReference(SqlAliasBase sqlAliasBase) {
		return new TableReference( getPrimaryTable(), sqlAliasBase.generateNewAlias() );
	}

	private void resolveTableReferenceJoins(
			TableReference rootTableReference,
			SqlAliasBase sqlAliasBase,
			TableGroupContext context,
			Consumer<TableReferenceJoin> collector) {

		for ( JoinedTableBinding joinedTableBinding : getSecondaryTableBindings() ) {
			collector.accept( createTableReferenceJoin( joinedTableBinding, rootTableReference, sqlAliasBase, context ) );
		}
	}

	protected TableReferenceJoin createTableReferenceJoin(
			JoinedTableBinding joinedTableBinding,
			TableReference rootTableReference,
			SqlAliasBase sqlAliasBase,
			TableGroupContext context) {
		final TableReference joinedTableReference = new TableReference(
				joinedTableBinding.getReferringTable(),
				sqlAliasBase.generateNewAlias()
		);

		return new TableReferenceJoin(
				joinedTableBinding.isOptional()
						? JoinType.LEFT
						: context.getTableReferenceJoinType(),
				joinedTableReference,
				generateJoinPredicate( rootTableReference, joinedTableReference, joinedTableBinding.getJoinForeignKey() )
		);
	}

	private Predicate generateJoinPredicate(
			TableReference rootTableReference,
			TableReference joinedTableReference,
			ForeignKey joinForeignKey) {
		assert rootTableReference.getTable() == joinForeignKey.getTargetTable();
		assert joinedTableReference.getTable() == joinForeignKey.getReferringTable();
		assert !joinForeignKey.getColumnMappings().getColumnMappings().isEmpty();

		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

		for ( ForeignKey.ColumnMappings.ColumnMapping columnMapping : joinForeignKey.getColumnMappings().getColumnMappings() ) {
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
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {
		final TableReference root = resolvePrimaryTableReference( sqlAliasBase );
		joinCollector.addRoot( root );
		resolveTableReferenceJoins( root, sqlAliasBase, tableGroupContext, joinCollector::collectTableReferenceJoin );
	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		assert navigableReference instanceof EntityValuedNavigableReference;

		return new EntityQueryResultImpl(
				(EntityValuedNavigable) navigableReference.getNavigable(),
				resultVariable,
				buildSqlSelectionMappings( navigableReference, creationContext ),
				( (EntityValuedNavigableReference) navigableReference ).getLockMode(),
				navigableReference.getNavigablePath(),
				creationContext
		);
	}

	// todo (6.0) : we need some way here to limit which attributes are rendered as how "deep" we render them
	//		* which to render comes down to bytecode enhanced laziness
	//		* how deep (associations) comes down to fetching


	private EntitySqlSelectionMappings buildSqlSelectionMappings(
			NavigableReference selectedExpression,
			QueryResultCreationContext resolutionContext) {
		final EntitySqlSelectionMappingsImpl.Builder mappings = new EntitySqlSelectionMappingsImpl.Builder();

		if ( getHierarchy().getRowIdDescriptor() != null ) {
			mappings.applyRowIdSqlSelection(
					resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
							resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
									selectedExpression.getSqlExpressionQualifier(),
									getHierarchy().getRowIdDescriptor().getBoundColumn()
							)
					)
			);
		}

		if ( getHierarchy().getDiscriminatorDescriptor() != null ) {
			mappings.applyDiscriminatorSqlSelection(
					resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
							resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
									selectedExpression.getSqlExpressionQualifier(),
									getHierarchy().getDiscriminatorDescriptor().getBoundColumn()
							)
					)
			);
		}

		if ( getHierarchy().getTenantDiscrimination() != null ) {
			mappings.applyTenantDiscriminatorSqlSelection(
					resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
							resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
									selectedExpression.getSqlExpressionQualifier(),
									getHierarchy().getTenantDiscrimination().getBoundColumn()
							)
					)
			);
		}

		mappings.applyIdSqlSelectionGroup(
				getHierarchy().getIdentifierDescriptor().resolveSqlSelectionGroup(
						selectedExpression.getSqlExpressionQualifier(),
						resolutionContext
				)
		);

		getPersistentAttributes().forEach(
				persistentAttribute -> {
					mappings.applyAttributeSqlSelectionGroup(
							persistentAttribute,
							persistentAttribute.resolveSqlSelectionGroup(
									selectedExpression.getSqlExpressionQualifier(),
									resolutionContext
							)
					);
				}
		);

		return mappings.create();
	}

	@Override
	public String getRootTableName() {
		return ( (PhysicalTable) rootTable ).getTableName().render( dialect );
	}

	@Override
	public String[] getRootTableIdentifierColumnNames() {
		final List<PhysicalColumn> columns = rootTable.getPrimaryKey().getColumns();
		String[] columnNames = new String[columns.size()];
		int i = 0;
		for ( PhysicalColumn column : columns ) {
			columnNames[i] = column.getName().render( dialect );
			i++;
		}
		return columnNames;
	}

	@Override
	public String getVersionColumnName() {
		return ( (PhysicalColumn) getHierarchy().getVersionDescriptor().getBoundColumn() )
				.getName()
				.render( dialect );
	}

	@Override
	public boolean hasNaturalIdentifier() {
		return getHierarchy().getNaturalIdDescriptor() != null;
	}

	@Override
	public Object instantiate(Serializable id, SharedSessionContractImplementor session) {
		final J instance = instantiator.instantiate( session );
		setIdentifier( instance, id, session );
		return instance;
	}

	@Override
	public boolean isInstance(Object object) {
		return instantiator.isInstance( object, getFactory() );
	}

	@Override
	public void setPropertyValues(Object object, Object[] values) {
		// todo (6.0) : hook in ReflectionOptimizer.AccessOptimizer
		super.setPropertyValues( object, values );
	}

	@Override
	public Serializable getIdentifier(Object entity) throws HibernateException {
		// todo (6.0) : for now we assume a basic identifier or aggregated composite
		//		one with a single attribute
		return (Serializable) ( (SingularPersistentAttribute) getHierarchy().getIdentifierDescriptor() )
				.getPropertyAccess()
				.getGetter()
				.get( entity );
	}

	@Override
	public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return getIdentifier( entity );
	}

	@Override
	public void setIdentifier(
			Object entity,
			Serializable id,
			SharedSessionContractImplementor session) {
		// todo (6.0) : for now we assume a basic identifier or aggregated composite
		//		one with a single attribute
		( (SingularPersistentAttribute) getHierarchy().getIdentifierDescriptor() )
				.getPropertyAccess()
				.getSetter()
				.set( entity, id, session.getFactory() );
	}

	@Override
	public void resetIdentifier(
			Object entity,
			Serializable currentId,
			Object currentVersion,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Object getVersion(Object object) throws HibernateException {
		return null;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"%s(`%s`)@%s",
				getClass().getSimpleName(),
				getEntityName(),
				hashCode()
		);
	}
}
