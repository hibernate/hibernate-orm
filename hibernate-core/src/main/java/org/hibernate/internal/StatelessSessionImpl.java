/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.sql.Connection;
import javax.transaction.SystemException;

import org.hibernate.CacheMode;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SessionException;
import org.hibernate.StatelessSession;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StatelessSessionImpl extends AbstractSharedSessionContract implements StatelessSession {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StatelessSessionImpl.class );

	private static LoadQueryInfluencers NO_INFLUENCERS = new LoadQueryInfluencers( null ) {
		@Override
		public String getInternalFetchProfile() {
			return null;
		}

		@Override
		public void setInternalFetchProfile(String internalFetchProfile) {
		}
	};

	private PersistenceContext temporaryPersistenceContext = new StatefulPersistenceContext( this );

	private boolean connectionProvided;

	StatelessSessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );
		connectionProvided = options.getConnection() != null;
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object insert(Object entity) {
		checkOpen();
		return insert( null, entity );
	}

	@Override
	public Object insert(String entityName, Object entity) {
		checkOpen();

		EntityTypeDescriptor descriptor = getEntityDescriptor( entityName, entity );
		Object id = descriptor.getIdentifierDescriptor().getIdentifierValueGenerator().generate( this, entity );
		Object[] state = descriptor.getPropertyValues( entity );

		final VersionDescriptor versionDescriptor = descriptor.getHierarchy().getVersionDescriptor();
		if ( versionDescriptor != null ) {
			boolean substitute = Versioning.seedVersion(
					state,
					versionDescriptor,
					this
			);
			if ( substitute ) {
				descriptor.setPropertyValues( entity, state );
			}
		}
		if ( id == IdentifierGeneratorHelper.POST_INSERT_INDICATOR ) {
			id = descriptor.insert( state, entity, this );
		}
		else {
			descriptor.insert( id, state, entity, this );
		}
		descriptor.setIdentifier( entity, id, this );
		return id;
	}


	// deletes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void delete(Object entity) {
		checkOpen();
		delete( null, entity );
	}

	@Override
	public void delete(String entityName, Object entity) {
		checkOpen();
		EntityTypeDescriptor descriptor = getEntityDescriptor( entityName, entity );
		Object id = descriptor.getIdentifier( entity, this );
		Object version = descriptor.getVersion( entity );
		descriptor.delete( id, version, entity, this );
	}


	// updates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void update(Object entity) {
		checkOpen();
		update( null, entity );
	}

	@Override
	public void update(String entityName, Object entity) {
		checkOpen();
		EntityTypeDescriptor entityDescriptor = getEntityDescriptor( entityName, entity );
		Object id = entityDescriptor.getHierarchy().getIdentifierDescriptor().extractIdentifier( entity, this );
		Object[] state = entityDescriptor.getPropertyValues( entity );
		Object oldVersion;
		final VersionDescriptor<Object, Object> versionDescriptor = entityDescriptor.getHierarchy().getVersionDescriptor();
		if ( versionDescriptor != null ) {
			oldVersion = entityDescriptor.getVersion( entity );
			final VersionSupport versionSupport = versionDescriptor.getVersionSupport();
			Object newVersion = Versioning.increment( oldVersion, versionSupport, this );
			Versioning.setVersion( state, newVersion, entityDescriptor );
			entityDescriptor.setPropertyValues( entity, state );
		}
		else {
			oldVersion = null;
		}
		entityDescriptor.update( id, state, null, false, null, oldVersion, entity, null, this );
	}


	// loading ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object get(Class entityClass, Object id) {
		return get( entityClass.getName(), id );
	}

	@Override
	public Object get(Class entityClass, Object id, LockMode lockMode) {
		return get( entityClass.getName(), id, lockMode );
	}

	@Override
	public Object get(String entityName, Object id) {
		return get( entityName, id, LockMode.NONE );
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) {
		checkOpen();

		final LockOptions lockOptions = new LockOptions( getNullSafeLockMode( lockMode ) );

		final Object result = getFactory().getMetamodel()
				.findEntityDescriptor( entityName )
				.getSingleIdLoader()
				.load( id, lockOptions, this );

		if ( temporaryPersistenceContext.isLoadFinished() ) {
			temporaryPersistenceContext.clear();
		}

		return result;
	}

	@Override
	public void refresh(Object entity) {
		refresh( bestGuessEntityName( entity ), entity, LockMode.NONE );
	}

	@Override
	public void refresh(String entityName, Object entity) {
		refresh( entityName, entity, LockMode.NONE );
	}

	@Override
	public void refresh(Object entity, LockMode lockMode) {
		refresh( bestGuessEntityName( entity ), entity, lockMode );
	}

	@Override
	public void refresh(String entityName, Object entity, LockMode lockMode) {
		final EntityTypeDescriptor entityDescriptor = this.getEntityDescriptor( entityName, entity );
		final Object id = entityDescriptor.getIdentifier( entity, this );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Refreshing transient {0}", MessageHelper.infoString( entityDescriptor, id, this.getFactory() ) );
		}
		// TODO : can this ever happen???
//		EntityKey key = new EntityKey( id, entityDescriptor, source.getEntityMode() );
//		if ( source.getPersistenceContext().getEntry( key ) != null ) {
//			throw new PersistentObjectException(
//					"attempted to refresh transient instance when persistent " +
//					"instance was already associated with the Session: " +
//					MessageHelper.infoString( entityDescriptor, id, source.getFactory() )
//			);
//		}

		if ( entityDescriptor.canWriteToCache() ) {
			final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
			if ( cacheAccess != null ) {
				final Object ck = cacheAccess.generateCacheKey(
						id,
						entityDescriptor.getHierarchy(),
						getFactory(),
						getTenantIdentifier()
				);
				cacheAccess.evict( ck );
			}
		}
		final LoadQueryInfluencers.InternalFetchProfileType previouslyEnabledInternalFetchProfileType =getLoadQueryInfluencers().getEnabledInternalFetchProfileType();
		getLoadQueryInfluencers().setEnabledInternalFetchProfileType( LoadQueryInfluencers.InternalFetchProfileType.REFRESH );

		final Object result ;
		try {
			final LockOptions lockOptions = new LockOptions( getNullSafeLockMode( lockMode ) );
			result = entityDescriptor.getSingleIdLoader().load( id, lockOptions, this );
		}
		finally {
			getLoadQueryInfluencers().setEnabledInternalFetchProfileType( previouslyEnabledInternalFetchProfileType );
		}

		UnresolvableObjectException.throwIfNull( result, id, entityDescriptor.getEntityName() );
	}

	@Override
	public Object immediateLoad(String entityName, Object id)
			throws HibernateException {
		throw new SessionException( "proxies cannot be fetched by a stateless session" );
	}

	@Override
	public void initializeCollection(
			PersistentCollection collection,
			boolean writing) throws HibernateException {
		throw new SessionException( "collections cannot be fetched by a stateless session" );
	}

	@Override
	public Object instantiate(
			String entityName,
			Object id) throws HibernateException {
		checkOpen();
		return getFactory().getMetamodel().findEntityDescriptor( entityName ).instantiate( id, this );
	}

	@Override
	public Object internalLoad(
			String entityName,
			Object id,
			boolean eager,
			boolean nullable) throws HibernateException {
		checkOpen();
		EntityTypeDescriptor descriptor = getFactory().getMetamodel().findEntityDescriptor( entityName );
		// first, try to load it from the temp PC associated to this SS
		Object loaded = temporaryPersistenceContext.getEntity( generateEntityKey( id, descriptor ) );
		if ( loaded != null ) {
			// we found it in the temp PC.  Should indicate we are in the midst of processing a result set
			// containing eager fetches via join fetch
			return loaded;
		}
		if ( !eager && descriptor.hasProxy() ) {
			// if the metadata allowed proxy creation and caller did not request forceful eager loading,
			// generate a proxy
			return descriptor.createProxy( id, this );
		}
		// otherwise immediately materialize it
		return get( entityName, id );
	}


	@Override
	protected Object load(String entityName, Object identifier) {
		return internalLoad(
				entityName,
				identifier,
				false,
				false
		);
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return getFactory().getSessionFactoryOptions().isAutoCloseSessionEnabled();
	}

	@Override
	public boolean shouldAutoClose() {
		return isAutoCloseSessionEnabled() && !isClosed();
	}


	private boolean isFlushModeNever() {
		return false;
	}

	private void managedClose() {
		if ( isClosed() ) {
			throw new SessionException( "Session was already closed!" );
		}
		close();
	}

	private void managedFlush() {
		checkOpen();
		getJdbcCoordinator().executeBatch();
	}

	@Override
	public String bestGuessEntityName(Object object) {
		if ( object instanceof HibernateProxy ) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}
		return guessEntityName( object );
	}

	@Override
	public Connection connection() {
		checkOpen();
		return getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
	}

	@Override
	public CacheMode getCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	public void setCacheMode(CacheMode cm) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFlushMode(FlushMode fm) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDontFlushFromFind() {
		return 0;
	}

	@Override
	public Object getContextEntityIdentifier(Object object) {
		checkOpen();
		return null;
	}

	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		checkOpen();
		return entity.getClass().getName();
	}

	@Override
	public EntityTypeDescriptor getEntityDescriptor(String entityName, Object object) throws HibernateException {
		checkOpen();
		if ( entityName == null ) {
			return getFactory().getMetamodel().findEntityDescriptor( guessEntityName( object ) );
		}
		else {
			return getFactory().getMetamodel().findEntityDescriptor( entityName ).getSubclassEntityDescriptor( object, getFactory() );
		}
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		checkOpen();
		return null;
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return temporaryPersistenceContext;
	}

	@Override
	public void setAutoClear(boolean enabled) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEventSource() {
		return false;
	}

	public boolean isDefaultReadOnly() {
		return false;
	}

	public void setDefaultReadOnly(boolean readOnly) throws HibernateException {
		if ( readOnly ) {
			throw new UnsupportedOperationException();
		}
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////

	//TODO: COPY/PASTE FROM SessionImpl, pull up!

	public void afterOperation(boolean success) {
		if ( !isTransactionInProgress() ) {
			getJdbcCoordinator().afterTransaction();
		}
	}

	@Override
	public void afterScrollOperation() {
		temporaryPersistenceContext.clear();
	}

	@Override
	public void flush() {
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return NO_INFLUENCERS;
	}

	@Override
	public void afterTransactionBegin() {

	}

	@Override
	public void beforeTransactionCompletion() {
		flushBeforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		if ( shouldAutoClose() && !isClosed() ) {
			managedClose();
		}
	}

	@Override
	public boolean isTransactionInProgress() {
		return connectionProvided || super.isTransactionInProgress();
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		boolean flush = false;
		try {
			flush = (
					!isClosed()
							&& !isFlushModeNever()
							&& !JtaStatusHelper.isRollback(
							getJtaPlatform().getCurrentStatus()
					) );
		}
		catch (SystemException se) {
			throw new HibernateException( "could not determine transaction status in beforeCompletion()", se );
		}
		if ( flush ) {
			managedFlush();
		}
	}

	private JtaPlatform getJtaPlatform() {
		return getFactory().getServiceRegistry().getService( JtaPlatform.class );
	}

	private LockMode getNullSafeLockMode(LockMode lockMode) {
		return lockMode == null ? LockMode.NONE : lockMode;
	}
}
