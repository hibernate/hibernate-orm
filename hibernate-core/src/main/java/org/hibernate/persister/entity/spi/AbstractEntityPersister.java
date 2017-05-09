/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.spi;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterHelper;
import org.hibernate.persister.collection.spi.CollectionElement;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinablePersistentAttribute;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.entity.internal.AbstractIdentifiableType;
import org.hibernate.persister.entity.internal.IdentifierDescriptorCompositeAggregated;
import org.hibernate.persister.entity.internal.IdentifierDescriptorSimple;
import org.hibernate.persister.exec.spi.EntityLocker;
import org.hibernate.persister.exec.spi.MultiIdEntityLoader;
import org.hibernate.persister.exec.spi.SingleIdEntityLoader;
import org.hibernate.persister.exec.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.persister.queryable.spi.NavigableReferenceInfo;
import org.hibernate.persister.queryable.spi.RootTableGroupContext;
import org.hibernate.persister.queryable.spi.SqlAliasBaseResolver;
import org.hibernate.persister.queryable.spi.TableGroupJoinContext;
import org.hibernate.persister.queryable.spi.TableGroupResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReferenceExpression;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityPersister<T>
		extends AbstractIdentifiableType<T>
		implements EntityPersister<T> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractEntityPersister.class );

	private final SessionFactoryImplementor factory;

	// needed temporarily between construction of the persister and its afterInitialization call
	private final EntityRegionAccessStrategy cacheAccessStrategy;
	private final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy;

	private final NavigableRole navigableRole;
	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;

	private final EntityTuplizer tuplizer;

	@SuppressWarnings("UnnecessaryBoxing")
	public AbstractEntityPersister(
			EntityHierarchy entityHierarchy,
			EntityMapping entityMapping,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		super( entityHierarchy, resolveJavaTypeDescriptor( creationContext, entityMapping ) );

		this.factory = creationContext.getSessionFactory();
		this.cacheAccessStrategy = cacheAccessStrategy;
		this.naturalIdRegionAccessStrategy = naturalIdRegionAccessStrategy;

		this.navigableRole = new NavigableRole( entityMapping.getEntityName() );

		if ( entityHierarchy.getEntityMode() == EntityMode.POJO ) {
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

	private static <T> IdentifiableJavaDescriptor<T> resolveJavaTypeDescriptor(
			PersisterCreationContext creationContext,
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
			PersisterCreationContext creationContext) {
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
	public NavigableSource getSource() {
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
	public String getTypeName() {
		return getJavaTypeDescriptor().getTypeName();
	}

	@Override
	public EntityPersister<T> getEntityPersister() {
		return this;
	}

	@Override
	public String getRolePrefix() {
		return getEntityName();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
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
		return getIdentifierDescriptor() instanceof IdentifierDescriptorSimple
				|| getIdentifierDescriptor() instanceof IdentifierDescriptorCompositeAggregated;
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
	public EntityTableGroup applyRootTableGroup(
			NavigableReferenceInfo navigableReferenceInfo,
			RootTableGroupContext tableGroupContext,
			SqlAliasBaseResolver sqlAliasBaseResolver) {
		assert navigableReferenceInfo.getReferencedNavigable() instanceof EntityValuedNavigable;
		assert ( (EntityValuedNavigable) navigableReferenceInfo ).getEntityPersister() == this;

		final EntityTableGroup group = createEntityTableGroup(
				navigableReferenceInfo,
				tableGroupContext.getTableSpace(),
				sqlAliasBaseResolver
		);

		tableGroupContext.getTableSpace().setRootTableGroup( group );

		// todo (6.0) - apply filters - which needs access to Session, or at least its LoadQueryInfluencers
		//		the filter conditions would be added to the SQL-AST's where-clause via tableGroupContext
		//		for now, add null, this is just here as a placeholder
		tableGroupContext.addRestriction( null );

		return group;
	}

	@Override
	public TableGroupJoin applyTableGroupJoin(
			NavigableReferenceInfo navigableReferenceInfo,
			SqmJoinType joinType,
			TableGroupJoinContext tableGroupJoinContext,
			TableGroupResolver tableGroupResolutionContext,
			SqlAliasBaseResolver sqlAliasBaseResolver) {
		assert navigableReferenceInfo.getReferencedNavigable() instanceof EntityValuedNavigable;
		assert navigableReferenceInfo.getReferencedNavigable() instanceof JoinablePersistentAttribute
				|| navigableReferenceInfo.getReferencedNavigable() instanceof CollectionElement;
		assert ( (EntityValuedNavigable) navigableReferenceInfo ).getEntityPersister() == this;
		assert navigableReferenceInfo.getSourceReferenceInfo() != null;

		final EntityTableGroup group = createEntityTableGroup(
				navigableReferenceInfo,
				tableGroupJoinContext.getTableSpace(),
				sqlAliasBaseResolver
		);

		// todo (6.0) - apply filters - but which again needs access to Session, or at least its LoadQueryInfluencers
		//		- see above note in #applyTableGroup
		//		- here, though, we'd apply the predicate to the TableGroupJoin's predicate instead

		// create the join predicate
		final Predicate joinPredicate;
		TableGroup lhsTableGroup = tableGroupResolutionContext.resolveTableGroup( navigableReferenceInfo.getSourceReferenceInfo().getUniqueIdentifier() );
		final List<JoinColumnMapping> joinColumnMappings = ( (JoinablePersistentAttribute<?, ?>) navigableReferenceInfo.getReferencedNavigable() )
				.getJoinColumnMappings();
		if ( joinColumnMappings.size() == 1 ) {
			joinPredicate = createJoinPredicate( joinColumnMappings.get( 0 ), lhsTableGroup, group );
		}
		else {
			joinPredicate = new Junction( Junction.Nature.CONJUNCTION );
			for ( JoinColumnMapping joinColumnMapping : joinColumnMappings ) {
				( (Junction) joinPredicate ).add( createJoinPredicate( joinColumnMapping, lhsTableGroup, group ) );
			}
		}

		// todo (6.0) : the null here is the join predicate - need to build it
		//		however the
		// todo (6.0) : create a "FilterableNavigable" or somesuch
		TableGroupJoin tableGroupJoin = new TableGroupJoin(
				joinType,
				group,
				joinPredicate
		);

		tableGroupJoinContext.getTableSpace().addJoinedTableGroup( tableGroupJoin );

		return tableGroupJoin;

	}

	private Predicate createJoinPredicate(
			JoinColumnMapping joinColumnMapping,
			TableGroup lhsTableGroup,
			EntityTableGroup rhsTableGroup) {
		return new RelationalPredicate(
				RelationalPredicate.Operator.EQUAL,
				new ColumnReferenceExpression(
						lhsTableGroup.resolveColumnBinding( joinColumnMapping.getLeftHandSideColumn() )
				),
				new ColumnReferenceExpression(
						rhsTableGroup.resolveColumnBinding( joinColumnMapping.getRightHandSideColumn() )
				)
		);
	}
}
