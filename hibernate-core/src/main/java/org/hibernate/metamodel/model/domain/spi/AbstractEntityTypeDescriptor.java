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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.MappedJoin;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterHelper;
import org.hibernate.loader.internal.StandardMultiIdEntityLoader;
import org.hibernate.loader.internal.StandardNaturalIdLoader;
import org.hibernate.loader.internal.StandardSingleIdEntityLoader;
import org.hibernate.loader.spi.EntityLocker;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.loader.spi.MultiIdLoaderSelectors;
import org.hibernate.loader.spi.NaturalIdLoader;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.entity.EntityHierarchyImpl;
import org.hibernate.metamodel.model.domain.internal.entity.EntityIdentifierCompositeAggregatedImpl;
import org.hibernate.metamodel.model.domain.internal.entity.EntityIdentifierSimpleImpl;
import org.hibernate.metamodel.model.domain.internal.SqlAliasStemHelper;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.results.internal.domain.entity.EntityResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityTypeDescriptor<J>
		extends AbstractIdentifiableType<J>
		implements Lockable<J> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractEntityTypeDescriptor.class );

	private final SessionFactoryImplementor factory;
	private final EntityHierarchy hierarchy;

	private final NavigableRole navigableRole;

	private final Table rootTable;
	private final List<JoinedTableBinding> secondaryTableBindings;

	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;
	private final Instantiator<J> instantiator;

	private final String sqlAliasStem;

	private final Dialect dialect;

	private final boolean canReadFromCache;
	private final boolean canWriteToCache;

	private final boolean hasProxy;
	private final Class proxyInterface;

	private ProxyFactory proxyFactory;
	private boolean canIdentityInsertBeDelayed;

	protected final ExecuteUpdateResultCheckStyle rootUpdateResultCheckStyle;

	@SuppressWarnings("UnnecessaryBoxing")
	public AbstractEntityTypeDescriptor(
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
		rootUpdateResultCheckStyle = bootMapping.getUpdateResultCheckStyle();
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
				creationContext.getSessionFactory().getSessionFactoryOptions().getBytecodeProvider()
		);

		log.debugf(
				"Instantiated persister [%s] for entity [%s (%s)]",
				this,
				bootMapping.getEntityName(),
				bootMapping.getJpaEntityName()
		);

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( bootMapping.getEntityName() );
		this.dialect = factory.getServiceRegistry().getService( JdbcServices.class ).getDialect();

		if ( creationContext.getSessionFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled() ) {
			PersistentClass persistentClass = (PersistentClass) bootMapping;
			this.canWriteToCache = persistentClass.isCached();
			this.canReadFromCache = determineCanReadFromCache( persistentClass );
		}
		else {
			this.canWriteToCache = false;
			this.canReadFromCache = false;
		}

		// Handle any filters applied to the class level
		this.filterHelper = new FilterHelper( bootMapping.getFilters(), factory );

		this.hasProxy = bootMapping.hasProxy() && !bytecodeEnhancementMetadata.isEnhancedForLazyLoading();
		proxyInterface = bootMapping.getProxyInterface();

		creationContext.registerNavigable( this, bootMapping );
	}

	@SuppressWarnings("unchecked")
	private boolean determineCanReadFromCache(PersistentClass persistentClass) {
		if ( persistentClass.isCached() ) {
			return true;
		}

		final Iterator<Subclass> subclassIterator = persistentClass.getSubclassIterator();
		while ( subclassIterator.hasNext() ) {
			final Subclass subclass = subclassIterator.next();
			if ( subclass.isCached() ) {
				return true;
			}
		}
		return false;
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
		final Collection<MappedJoin> mappedJoins = entityMapping.getMappedJoins();
		if ( mappedJoins.size() <= 0 ) {
			return Collections.emptyList();
		}

		if ( mappedJoins.size() == 1 ) {
			return Collections.singletonList(
					generateJoinedTableBinding( mappedJoins.iterator().next(), creationContext )
			);
		}

		final ArrayList<JoinedTableBinding> bindings = new ArrayList<>();
		for ( MappedJoin mappedJoin : mappedJoins ) {
			bindings.add(
					generateJoinedTableBinding( mappedJoin, creationContext )
			);
		}
		return bindings;
	}

	private JoinedTableBinding generateJoinedTableBinding(
			MappedJoin bootJoinTable,
			RuntimeModelCreationContext creationContext) {
		final Table joinedTable = resolveTable( bootJoinTable.getMappedTable(), creationContext );

		// todo (6.0) : resolve "join predicate" as ForeignKey.ColumnMappings
		//		see note on MappedJoin regarding what to expose there


		return new JoinedTableBinding(
				// NOTE : for secondary tables, it is the secondary table that is
				//		the referring table
				joinedTable,
				getPrimaryTable(),
				creationContext.getDatabaseObjectResolver().resolveForeignKey( bootJoinTable.getJoinMapping() ),
				bootJoinTable.isOptional(),
				bootJoinTable.isInverse(),
				bootJoinTable.getUpdateResultCheckStyle()
		);
	}

	private static <T> IdentifiableJavaDescriptor<T> resolveJavaTypeDescriptorFromJavaTypeMapping(
			EntityMapping entityMapping) {
		return (IdentifiableJavaDescriptor<T>) entityMapping.getJavaTypeMapping().getJavaTypeDescriptor();
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
	}

	@Override
	public void postInitialization(RuntimeModelCreationContext creationContext) {
		this.singleIdLoader = new StandardSingleIdEntityLoader<>( this );

		resolveIdentityInsertDelayable();
	}

	@Override
	public boolean finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		super.finishInitialization( bootDescriptor, creationContext );

		log.debugf(
				"Completed initialization of descriptor [%s] for entity [%s (%s)]",
				this,
				getJavaTypeDescriptor().getEntityName(),
				getJavaTypeDescriptor().getJpaEntityName()
		);

		if ( hasProxy ) {
			this.proxyFactory = getRepresentationStrategy().generateProxyFactory( this, creationContext );
		}

		return true;
	}

	@Override
	public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
		if ( log.isTraceEnabled() ) {
			log.tracev(
					"Getting current persistent state for: {0}", MessageHelper.infoString(
							this,
							id,
							getFactory()
					)
			);
		}
		return getSingleIdLoader().loadDatabaseSnapshot( id, session );
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
	public boolean canReadFromCache() {
		return canReadFromCache;
	}

	@Override
	public boolean canWriteToCache() {
		return canWriteToCache;
	}

	@Override
	public boolean hasProxy() {
		return hasProxy;
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
	public EntityTypeDescriptor<J> getEntityDescriptor() {
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
		return getHierarchy().getIdentifierDescriptor().asAttribute( type );
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
	public SimpleTypeDescriptor<?> getIdType() {
		return getHierarchy().getIdentifierDescriptor().getNavigableType();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	private final SingleIdEntityLoader customQueryLoader = null;
	private StandardSingleIdEntityLoader<J> singleIdLoader;

	private final FilterHelper filterHelper;
	private final Set<String> affectingFetchProfileNames = new HashSet<>();

	@Override
	@SuppressWarnings("unchecked")
	public SingleIdEntityLoader getSingleIdLoader() {
		if ( customQueryLoader != null ) {
			// if the user specified that we should use a custom query for loading this entity, we need
			// 		to always use that custom loader.
			return customQueryLoader;
		}

		return singleIdLoader;
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers loadQueryInfluencers) {
		assert filterHelper != null;
		return loadQueryInfluencers.hasEnabledFilters()
				&& filterHelper.isAffectedBy( loadQueryInfluencers.getEnabledFilters() );
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers loadQueryInfluencers) {
		for ( String s : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
			if ( affectingFetchProfileNames.contains( s ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers loadQueryInfluencers) {
		return loadQueryInfluencers.getFetchGraph() != null
				|| loadQueryInfluencers.getLoadGraph() != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		if ( ! getBindableJavaType().isAssignableFrom( subType ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Entity type [%s] cannot be treated as requested sub-type [%s]",
							getName(),
							subType.getName()
					)
			);
		}

		return new SubGraphImpl( this, true, getTypeConfiguration().getSessionFactory() );
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph() {
		return makeSubGraph( getBindableJavaType() );
	}

	@SuppressWarnings("WeakerAccess")
	protected SingleIdEntityLoader createSingleIdLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		return singleIdLoader;
	}

	@Override
	public NaturalIdLoader getNaturalIdLoader() {
		if ( ! hasNaturalIdentifier() ) {
			throw new UnsupportedOperationException( "Entity [" + getEntityName() + "] does not define a natural-id" );
		}

		// todo (6.0) : can this be cached like `singleIdLoader`?
		return new StandardNaturalIdLoader( this );
	}

	@Override
	public MultiIdEntityLoader getMultiIdLoader(MultiIdLoaderSelectors selectors) {
		if ( customQueryLoader != null ) {
			throw new HibernateException(
					"Cannot perform multi-id loading on an entity defined with a custom load query : " + getEntityName()
			);
		}

		// todo (6.0) : maybe cache the QueryResult reference?
		// todo (6.0) : or cache the StandardMultiIdEntityLoader and have it cache things appropriately internally

		return new StandardMultiIdEntityLoader( this, selectors );
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
		resolveTableReferenceJoins( primaryTableReference, sqlAliasBase, tableGroupContext.getTableReferenceJoinType(), joins::add );

		final EntityTableGroup group = new EntityTableGroup(
				info.getUniqueIdentifier(),
				tableGroupContext.getTableSpace(),
				this,
				tableGroupContext.getLockOptions().getEffectiveLockMode( info.getIdentificationVariable() ),
				info.getNavigablePath(),
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
		return new TableReference( getPrimaryTable(), sqlAliasBase.generateNewAlias(), false );
	}

	private void resolveTableReferenceJoins(
			TableReference rootTableReference,
			SqlAliasBase sqlAliasBase,
			JoinType joinType,
			Consumer<TableReferenceJoin> collector) {

		for ( JoinedTableBinding joinedTableBinding : getSecondaryTableBindings() ) {
			collector.accept( createTableReferenceJoin( joinedTableBinding, rootTableReference, joinType, sqlAliasBase ) );
		}
	}

	protected TableReferenceJoin createTableReferenceJoin(
			JoinedTableBinding joinedTableBinding,
			TableReference rootTableReference,
			JoinType joinType,
			SqlAliasBase sqlAliasBase) {
		final TableReference joinedTableReference = new TableReference(
				joinedTableBinding.getReferringTable(),
				sqlAliasBase.generateNewAlias(),
				joinedTableBinding.isOptional()
		);

		return new TableReferenceJoin(
				joinedTableBinding.isOptional()
						? JoinType.LEFT
						: joinType,
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
			TableReferenceJoinCollector joinCollector) {
		final TableReference root = resolvePrimaryTableReference( sqlAliasBase );
		joinCollector.addPrimaryReference( root );
		resolveTableReferenceJoins( root, sqlAliasBase, joinType, joinCollector::addSecondaryReference );
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		assert navigableReference instanceof EntityValuedNavigableReference;
		final EntityValuedNavigableReference entityValuedReference = (EntityValuedNavigableReference) navigableReference;

		final EntityResultImpl entityQueryResult = new EntityResultImpl(
				entityValuedReference.getNavigable(),
				resultVariable,
				// todo (6.0) : LockMode ?
				LockMode.READ,
				navigableReference.getNavigablePath(),
				creationContext,
				creationState
		);

		return entityQueryResult;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	// todo (6.0) : we need some way here to limit which attributes are rendered as how "deep" we render them
	//		* which to render comes down to bytecode enhanced laziness
	//		* how deep (associations) comes down to fetching

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
	public Object instantiate(Object id, SharedSessionContractImplementor session) {
		final J instance = instantiator.instantiate( session );
		setIdentifier( instance, id, session );
		return instance;
	}

	@Override
	public Object createProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return proxyFactory.getProxy( (Serializable) id, session );
	}

	@Override
	public boolean canIdentityInsertBeDelayed() {
		return canIdentityInsertBeDelayed;
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
	public void resetIdentifier(
			Object entity,
			Object currentId,
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

	@Override
	public void insert(
			Object id,
			Object[] fields,
			Object object,
			SharedSessionContractImplementor session) {
		insertInternal( id, fields, object, session );
	}

	protected Object insertInternal(
			Object id,
			Object[] fields,
			Object object,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Object insert(
			Object[] fields,
			Object object,
			SharedSessionContractImplementor session) {
		return insertInternal( null, fields, object, session );
	}

	public Class getProxyInterface() {
		return proxyInterface;
	}

	@Override
	public Class getConcreteProxyClass() {
		if ( getRepresentationStrategy().getMode().equals( RepresentationMode.POJO ) ) {
			return getProxyInterface();
		}
		else {
			return Map.class;
		}
	}

	private void resolveIdentityInsertDelayable() {
		// By default they can
		// The remainder of this method checks use cases where we shouldn't permit it.
		canIdentityInsertBeDelayed = true;

		if ( getIdentifierDescriptor().getIdentifierValueGenerator() instanceof PostInsertIdentifierGenerator ) {
			// if the descriptor's identifier is assigned by insert, we need to see if we must force non-delay mode.
			for ( NonIdPersistentAttribute attribute : getPersistentAttributes() ) {
				if ( isAttributeSelfReferencing( attribute ) ) {
					canIdentityInsertBeDelayed = false;
				}
			}
		}
	}

	private boolean isAttributeSelfReferencing(NonIdPersistentAttribute attribute) {
		if ( attribute.isAssociation() ) {
			if ( attribute.getPersistenceType().equals( PersistenceType.ENTITY ) ) {
				if ( getMappedClass().equals( attribute.getJavaType() ) ) {
					return true;
				}
			}
			else if ( attribute.isCollection() ) {
				// Association is a collection where owner needs identifier up-front
				final PersistentCollectionDescriptor collectionDescriptor = getFactory().getMetamodel()
						.getCollectionDescriptor( attribute.getNavigableRole() );
				if ( collectionDescriptor.isInverse() ) {
					if ( collectionDescriptor.findEntityOwnerDescriptor().equals( this ) ) {
						// todo (6.0) : Need to add check for the element persister's identifier generator
						//		if the generator is a ForeignGenerator or a SequenceStyleGenerator, return true
//						final QueryableCollection queryableCollection = (QueryableCollection) collectionPersister;
//						final IdentifierGenerator identifierGenerator = queryableCollection.getElementPersister().getIdentifierGenerator();
//						// todo - perhaps this can be simplified
//						if ( ( identifierGenerator instanceof ForeignGenerator ) || ( identifierGenerator instanceof SequenceStyleGenerator ) ) {
//							return true;
//						}
					}
				}
			}
		}
		else if ( attribute.getPersistenceType().equals( PersistenceType.EMBEDDABLE ) ) {
			final SingularPersistentAttributeEmbedded embedded = (SingularPersistentAttributeEmbedded) attribute;
			final EmbeddedTypeDescriptor<?> embeddedDescriptor = embedded.getEmbeddedDescriptor();
			for ( NonIdPersistentAttribute<?,?> embeddedAttribute : embeddedDescriptor.getPersistentAttributes() ) {
				if ( isAttributeSelfReferencing( embeddedAttribute ) ) {
					return true;
				}
			}
		}

		return false;
	}
}
