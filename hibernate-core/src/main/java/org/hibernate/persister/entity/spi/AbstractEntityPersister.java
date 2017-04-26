/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.spi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.entity.internal.AbstractIdentifiableType;
import org.hibernate.persister.entity.internal.IdentifierDescriptorCompositeAggregated;
import org.hibernate.persister.entity.internal.IdentifierDescriptorSimple;
import org.hibernate.persister.exec.spi.EntityLocker;
import org.hibernate.persister.exec.spi.MultiIdEntityLoader;
import org.hibernate.persister.exec.spi.SingleIdEntityLoader;
import org.hibernate.persister.exec.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.persister.queryable.spi.InFlightJdbcJdbcOperation;
import org.hibernate.persister.queryable.spi.NavigableReferenceInfo;
import org.hibernate.persister.queryable.spi.TableGroupResolutionContext;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.from.EntityTableGroup;
import org.hibernate.sql.ast.from.TableGroupJoin;
import org.hibernate.sql.ast.from.TableSpace;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.query.sqm.domain.type.SqmDomainTypeEntity;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataPojoImpl;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityPersister<T>
		extends AbstractIdentifiableType<T>
		implements EntityPersister<T>, SqmDomainTypeEntity {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractEntityPersister.class );

	private final SessionFactoryImplementor factory;

	// needed temporarily between construction of the persister and its afterInitialization call
	private final EntityRegionAccessStrategy cacheAccessStrategy;
	private final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy;

	private final NavigableRole navigableRole;
	private final BytecodeEnhancementMetadata bytecodeEnhancementMetadata;


	@SuppressWarnings("UnnecessaryBoxing")
	public AbstractEntityPersister(
			PersistentClass persistentClass,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		super( resolveJavaTypeDescriptor( creationContext, persistentClass ) );

		this.factory = creationContext.getSessionFactory();
		this.cacheAccessStrategy = cacheAccessStrategy;
		this.naturalIdRegionAccessStrategy = naturalIdRegionAccessStrategy;

		this.navigableRole = new NavigableRole( persistentClass.getEntityName() );

		if ( persistentClass.hasPojoRepresentation() ) {
			bytecodeEnhancementMetadata = BytecodeEnhancementMetadataPojoImpl.from( persistentClass );
		}
		else {
			bytecodeEnhancementMetadata = new BytecodeEnhancementMetadataNonPojoImpl( persistentClass.getEntityName() );
		}

		log.debugf(
				"Instantiated persister [%s] for entity [%s (%s)]",
				this,
				getJavaTypeDescriptor().getEntityName(),
				getJavaTypeDescriptor().getJpaEntityName()
		);
	}

	private static <T> IdentifiableJavaDescriptor<T> resolveJavaTypeDescriptor(
			PersisterCreationContext creationContext,
			PersistentClass persistentClass) {
		return (EntityJavaDescriptor<T>) creationContext.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( persistentClass.getEntityName() );
	}

	@Override
	public void finishInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeImplementor<? super T> superType,
			PersistentClass mappingDescriptor,
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
	public SqmDomainTypeEntity getExportedDomainType() {
		return this;
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
	public EntityTableGroup applyTableGroup(
			NavigableReferenceInfo navigableInfo,
			InFlightJdbcJdbcOperation inFlightJdbcJdbcOperation,
			TableSpace tableSpace,
			TableGroupResolutionContext tableGroupResolutionContext,
			SqlAliasBaseManager sqlAliasBaseManager) {
		final EntityTableGroup group = new EntityTableGroup(
				tableSpace,
				navigableInfo.getUniqueIdentifier(),
				// todo (6.0) - need a proper key into SqlAliasBaseManager
				//		- currently relies on SqmFrom which only makes sense from query parsing
				//		- but if not keyed on SqmFrom what is the correct thing to use - try to avoid
				//			"key object instantiations".  Could use the uid, but would need a way to
				//			resolve that to
				sqlAliasBaseManager.getSqlAliasBase( null ),
				this,
				navigableInfo.getNavigablePath()
		);

		throw new NotYetImplementedException(  );
	}

	@Override
	public TableGroupJoin applyTableGroupJoin(
			NavigableReferenceInfo navigableBindingInfo,
			SqmJoinType joinType,
			InFlightJdbcJdbcOperation inFlightJdbcJdbcOperation,
			TableSpace tableSpace,
			TableGroupResolutionContext tableGroupResolutionContext,
			SqlAliasBaseManager sqlAliasBaseManager) {
		throw new NotYetImplementedException(  );
	}
}
