/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.transaction.SystemException;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.SessionException;
import org.hibernate.StatelessSession;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.spi.ScrollableResultsImplementor;

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

	StatelessSessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return true;
	}

	// inserts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Serializable insert(Object entity) {
		checkOpen();
		return insert( null, entity );
	}

	@Override
	public Serializable insert(String entityName, Object entity) {
		checkOpen();
		EntityPersister persister = getEntityPersister( entityName, entity );
		Serializable id = persister.getIdentifierGenerator().generate( this, entity );
		Object[] state = persister.getPropertyValues( entity );
		if ( persister.isVersioned() ) {
			boolean substitute = Versioning.seedVersion(
					state,
					persister.getVersionProperty(),
					persister.getVersionType(),
					this
			);
			if ( substitute ) {
				persister.setPropertyValues( entity, state );
			}
		}
		if ( id == IdentifierGeneratorHelper.POST_INSERT_INDICATOR ) {
			id = persister.insert( state, entity, this );
		}
		else {
			persister.insert( id, state, entity, this );
		}
		persister.setIdentifier( entity, id, this );
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
		EntityPersister persister = getEntityPersister( entityName, entity );
		Serializable id = persister.getIdentifier( entity, this );
		Object version = persister.getVersion( entity );
		persister.delete( id, version, entity, this );
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
		EntityPersister persister = getEntityPersister( entityName, entity );
		Serializable id = persister.getIdentifier( entity, this );
		Object[] state = persister.getPropertyValues( entity );
		Object oldVersion;
		if ( persister.isVersioned() ) {
			oldVersion = persister.getVersion( entity );
			Object newVersion = Versioning.increment( oldVersion, persister.getVersionType(), this );
			Versioning.setVersion( state, newVersion, persister );
			persister.setPropertyValues( entity, state );
		}
		else {
			oldVersion = null;
		}
		persister.update( id, state, null, false, null, oldVersion, entity, null, this );
	}


	// loading ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object get(Class entityClass, Serializable id) {
		return get( entityClass.getName(), id );
	}

	@Override
	public Object get(Class entityClass, Serializable id, LockMode lockMode) {
		return get( entityClass.getName(), id, lockMode );
	}

	@Override
	public Object get(String entityName, Serializable id) {
		return get( entityName, id, LockMode.NONE );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) {
		checkOpen();

		Object result = getFactory().getMetamodel().entityPersister( entityName )
				.load( id, null, getNullSafeLockMode( lockMode ), this );
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
		final EntityPersister persister = this.getEntityPersister( entityName, entity );
		final Serializable id = persister.getIdentifier( entity, this );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Refreshing transient {0}", MessageHelper.infoString( persister, id, this.getFactory() ) );
		}
		// TODO : can this ever happen???
//		EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
//		if ( source.getPersistenceContext().getEntry( key ) != null ) {
//			throw new PersistentObjectException(
//					"attempted to refresh transient instance when persistent " +
//					"instance was already associated with the Session: " +
//					MessageHelper.infoString( persister, id, source.getFactory() )
//			);
//		}

		if ( persister.hasCache() ) {
			final EntityRegionAccessStrategy cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( id, persister, getFactory(), getTenantIdentifier() );
			cache.evict( ck );
		}
		String previousFetchProfile = this.getLoadQueryInfluencers().getInternalFetchProfile();
		Object result = null;
		try {
			this.getLoadQueryInfluencers().setInternalFetchProfile( "refresh" );
			result = persister.load( id, entity, getNullSafeLockMode( lockMode ), this );
		}
		finally {
			this.getLoadQueryInfluencers().setInternalFetchProfile( previousFetchProfile );
		}
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id)
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
			Serializable id) throws HibernateException {
		checkOpen();
		return getFactory().getMetamodel().entityPersister( entityName ).instantiate( id, this );
	}

	@Override
	public Object internalLoad(
			String entityName,
			Serializable id,
			boolean eager,
			boolean nullable) throws HibernateException {
		checkOpen();
		EntityPersister persister = getFactory().getMetamodel().entityPersister( entityName );
		// first, try to load it from the temp PC associated to this SS
		Object loaded = temporaryPersistenceContext.getEntity( generateEntityKey( id, persister ) );
		if ( loaded != null ) {
			// we found it in the temp PC.  Should indicate we are in the midst of processing a result set
			// containing eager fetches via join fetch
			return loaded;
		}
		if ( !eager && persister.hasProxy() ) {
			// if the metadata allowed proxy creation and caller did not request forceful eager loading,
			// generate a proxy
			return persister.createProxy( id, this );
		}
		// otherwise immediately materialize it
		return get( entityName, id );
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List listFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		throw new UnsupportedOperationException();
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
	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getQueryPlan( query, false );
		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return result;
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
	public Serializable getContextEntityIdentifier(Object object) {
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
	public EntityPersister getEntityPersister(String entityName, Object object)
			throws HibernateException {
		checkOpen();
		if ( entityName == null ) {
			return getFactory().getMetamodel().entityPersister( guessEntityName( object ) );
		}
		else {
			return getFactory().getMetamodel().entityPersister( entityName ).getSubclassEntityPersister( object, getFactory() );
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
	protected Object load(String entityName, Serializable identifier) {
		return null;
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

	@Override
	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getQueryPlan( query, false );
		boolean success = false;
		List results = Collections.EMPTY_LIST;
		try {
			results = plan.performList( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	public void afterOperation(boolean success) {
		if ( !isTransactionInProgress() ) {
			getJdbcCoordinator().afterTransaction();
		}
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		checkOpen();
		return new CriteriaImpl( persistentClass.getName(), alias, this );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		checkOpen();
		return new CriteriaImpl( entityName, alias, this );
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		checkOpen();
		return new CriteriaImpl( persistentClass.getName(), this );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		checkOpen();
		return new CriteriaImpl( entityName, this );
	}

	@Override
	public ScrollableResultsImplementor scroll(Criteria criteria, ScrollMode scrollMode) {
		// TODO: Is this guaranteed to always be CriteriaImpl?
		CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;

		checkOpen();
		String entityName = criteriaImpl.getEntityOrClassName();
		CriteriaLoader loader = new CriteriaLoader(
				getOuterJoinLoadable( entityName ),
				getFactory(),
				criteriaImpl,
				entityName,
				getLoadQueryInfluencers()
		);
		return loader.scroll( this, scrollMode );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List list(Criteria criteria) throws HibernateException {
		// TODO: Is this guaranteed to always be CriteriaImpl?
		CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;

		checkOpen();
		String[] implementors = getFactory().getMetamodel().getImplementors( criteriaImpl.getEntityOrClassName() );
		int size = implementors.length;

		CriteriaLoader[] loaders = new CriteriaLoader[size];
		for ( int i = 0; i < size; i++ ) {
			loaders[i] = new CriteriaLoader(
					getOuterJoinLoadable( implementors[i] ),
					getFactory(),
					criteriaImpl,
					implementors[i],
					getLoadQueryInfluencers()
			);
		}


		List results = Collections.EMPTY_LIST;
		boolean success = false;
		try {
			for ( int i = 0; i < size; i++ ) {
				final List currentResults = loaders[i].list( this );
				currentResults.addAll( results );
				results = currentResults;
			}
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	private OuterJoinLoadable getOuterJoinLoadable(String entityName) throws MappingException {
		EntityPersister persister = getFactory().getMetamodel().entityPersister( entityName );
		if ( !( persister instanceof OuterJoinLoadable ) ) {
			throw new MappingException( "class persister is not OuterJoinLoadable: " + entityName );
		}
		return (OuterJoinLoadable) persister;
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException {
		checkOpen();
		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		boolean success = false;
		List results;
		try {
			results = loader.list( this, queryParameters );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return results;
	}

	@Override
	public ScrollableResultsImplementor scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException {
		checkOpen();
		CustomLoader loader = new CustomLoader( customQuery, getFactory() );
		return loader.scroll( queryParameters, this );
	}

	@Override
	public ScrollableResultsImplementor scroll(String query, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		HQLQueryPlan plan = getQueryPlan( query, false );
		return plan.performScroll( queryParameters, this );
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
	public int executeNativeUpdate(
			NativeSQLQuerySpecification nativeSQLQuerySpecification,
			QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		queryParameters.validateParameters();
		NativeSQLQueryPlan plan = getNativeQueryPlan( nativeSQLQuerySpecification );

		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
		}
		temporaryPersistenceContext.clear();
		return result;
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
