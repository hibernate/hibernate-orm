/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.transaction.JoinableCMTTransaction;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.JTAHelper;

/**
 * @author <a href="mailto:gavin@hibernate.org">Gavin King</a>
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public abstract class AbstractEntityManagerImpl implements HibernateEntityManagerImplementor, Serializable {
	private static final Logger log = LoggerFactory.getLogger( AbstractEntityManagerImpl.class );

	private EntityManagerFactoryImpl entityManagerFactory;
	protected transient TransactionImpl tx = new TransactionImpl( this );
	protected PersistenceContextType persistenceContextType;
	private FlushModeType flushModeType = FlushModeType.AUTO;
	private PersistenceUnitTransactionType transactionType;
	private Map properties;

	protected AbstractEntityManagerImpl(
			EntityManagerFactoryImpl entityManagerFactory,
			PersistenceContextType type,
			PersistenceUnitTransactionType transactionType,
			Map properties) {
		this.entityManagerFactory = entityManagerFactory;
		this.persistenceContextType = type;
		this.transactionType = transactionType;
		this.properties = properties != null ? properties : CollectionHelper.EMPTY_MAP;
	}

	protected void postInit() {
		//register in Sync if needed
		if ( PersistenceUnitTransactionType.JTA.equals( transactionType ) ) {
			joinTransaction( true );
		}
		Object flushMode = properties.get( "org.hibernate.flushMode" );
		if ( flushMode != null ) {
			getSession().setFlushMode( ConfigurationHelper.getFlushMode( flushMode ) );
		}
		this.properties = null;
	}

	public Query createQuery(String jpaqlString) {
		try {
			return new QueryImpl<Object>( getSession().createQuery( jpaqlString ), this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public <T> TypedQuery<T> createQuery(String jpaqlString, Class<T> resultClass) {
		try {
			org.hibernate.Query hqlQuery = getSession().createQuery( jpaqlString );
			if ( hqlQuery.getReturnTypes().length != 1 ) {
				throw new IllegalArgumentException( "Cannot create TypedQuery for query with more than one return" );
			}
			if ( ! resultClass.isAssignableFrom( hqlQuery.getReturnTypes()[0].getReturnedClass() ) ) {
				throw new IllegalArgumentException(
						"Type specified for TypedQuery [" +
								resultClass.getName() +
								"] is incompatible with query return type [" +
								hqlQuery.getReturnTypes()[0].getReturnedClass() + "]"
				);
			}
			return new QueryImpl<T>( hqlQuery, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	private CriteriaQueryCompiler criteriaQueryCompiler;

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		if ( criteriaQueryCompiler == null ) {
			criteriaQueryCompiler = new CriteriaQueryCompiler( this );
		}
		return criteriaQueryCompiler.compile( criteriaQuery );
	}

	public Query createNamedQuery(String name) {
		try {
			org.hibernate.Query namedQuery = getSession().getNamedQuery( name );
			try {
				return new QueryImpl( namedQuery, this );
			}
			catch ( HibernateException he ) {
				throw convert( he );
			}
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( "Named query not found: " + name );
		}
	}

	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		try {
			org.hibernate.Query namedQuery = getSession().getNamedQuery( name );
			try {
				if ( namedQuery.getReturnTypes().length != 1 ) {
					throw new IllegalArgumentException( "Cannot create TypedQuery for query with more than one return" );
				}
				if ( ! resultClass.isAssignableFrom( namedQuery.getReturnTypes()[0].getReturnedClass() ) ) {
					throw new IllegalArgumentException(
							"Type specified for TypedQuery [" +
									resultClass.getName() +
									"] is incompatible with query return type [" +
									namedQuery.getReturnTypes()[0].getReturnedClass() + "]"
					);
				}
				return new QueryImpl<T>( namedQuery, this );
			}
			catch ( HibernateException he ) {
				throw convert( he );
			}
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( "Named query not found: " + name );
		}
	}


	public Query createNativeQuery(String sqlString) {
		try {
			SQLQuery q = getSession().createSQLQuery( sqlString );
			return new QueryImpl( q, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public Query createNativeQuery(String sqlString, Class resultClass) {
		try {
			SQLQuery q = getSession().createSQLQuery( sqlString );
			q.addEntity( "alias1", resultClass.getName(), LockMode.READ );
			return new QueryImpl( q, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		try {
			SQLQuery q = getSession().createSQLQuery( sqlString );
			q.setResultSetMapping( resultSetMapping );
			return new QueryImpl( q, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		try {
			return ( T ) getSession().load( entityClass, ( Serializable ) primaryKey );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( TypeMismatchException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	@SuppressWarnings("unchecked")
	public <A> A find(Class<A> entityClass, Object primaryKey) {
		try {
			return ( A ) getSession().get( entityClass, ( Serializable ) primaryKey );
		}
		catch ( ObjectDeletedException e ) {
			//the spec is silent about people doing remove() find() on the same PC
			return null;
		}
		catch ( ObjectNotFoundException e ) {
			//should not happen on the entity itself with get
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( TypeMismatchException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public <T> T find(Class<T> tClass, Object o, Map<String, Object> stringObjectMap) {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T find(Class<T> tClass, Object o, LockModeType lockModeType) {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public <T> T find(Class<T> tClass, Object o, LockModeType lockModeType, Map<String, Object> stringObjectMap) {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	private void checkTransactionNeeded() {
		if ( persistenceContextType == PersistenceContextType.TRANSACTION && !isTransactionInProgress() ) {
			//no need to mark as rollback, no tx in progress
			throw new TransactionRequiredException(
					"no transaction is in progress for a TRANSACTION type persistence context"
			);
		}
	}

	public void persist(Object entity) {
		checkTransactionNeeded();
		try {
			getSession().persist( entity );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	@SuppressWarnings("unchecked")
	public <A> A merge(A entity) {
		checkTransactionNeeded();
		try {
			return ( A ) getSession().merge( entity );
		}
		catch ( ObjectDeletedException sse ) {
			throw new IllegalArgumentException( sse );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public void remove(Object entity) {
		checkTransactionNeeded();
		try {
			getSession().delete( entity );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public void refresh(Object entity) {
		checkTransactionNeeded();
		try {
			if ( !getSession().contains( entity ) ) {
				throw new IllegalArgumentException( "Entity not managed" );
			}
			getSession().refresh( entity );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public void refresh(Object o, Map<String, Object> stringObjectMap) {
		//FIXME
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void refresh(Object o, LockModeType lockModeType) {
		//FIXME
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void refresh(Object o, LockModeType lockModeType, Map<String, Object> stringObjectMap) {
		//FIXME
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public boolean contains(Object entity) {
		try {
			if ( entity != null
					&& !( entity instanceof HibernateProxy )
					&& getSession().getSessionFactory().getClassMetadata( entity.getClass() ) == null ) {
				throw new IllegalArgumentException( "Not an entity:" + entity.getClass() );
			}
			return getSession().contains( entity );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public LockModeType getLockMode(Object o) {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setProperty(String s, Object o) {
		//FIXME
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public Map<String, Object> getProperties() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Set<String> getSupportedProperties() {
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void flush() {
		try {
			if ( !isTransactionInProgress() ) {
				throw new TransactionRequiredException( "no transaction is in progress" );
			}
			//adjustFlushMode();
			getSession().flush();
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	/**
	 * return a Session
	 *
	 * @throws IllegalStateException if the entity manager is closed
	 */
	public abstract Session getSession();

	/**
	 * Return a Session (even if the entity manager is closed).
	 *
	 * @return A session.
	 */
	protected abstract Session getRawSession();

	public EntityTransaction getTransaction() {
		if ( transactionType == PersistenceUnitTransactionType.JTA ) {
			throw new IllegalStateException( "A JTA EntityManager cannot use getTransaction()" );
		}
		return tx;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityManagerFactoryImpl getEntityManagerFactory() {
		return entityManagerFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaBuilder getCriteriaBuilder() {
		return getEntityManagerFactory().getCriteriaBuilder();
	}

	/**
	 * {@inheritDoc}
	 */
	public Metamodel getMetamodel() {
		return getEntityManagerFactory().getMetamodel();
	}

	public void setFlushMode(FlushModeType flushModeType) {
		this.flushModeType = flushModeType;
		if ( flushModeType == FlushModeType.AUTO ) {
			getSession().setFlushMode( FlushMode.AUTO );
		}
		else if ( flushModeType == FlushModeType.COMMIT ) {
			getSession().setFlushMode( FlushMode.COMMIT );
		}
		else {
			throw new AssertionFailure( "Unknown FlushModeType: " + flushModeType );
		}
	}

	public void clear() {
		//adjustFlushMode();
		try {
			getSession().clear();
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public void detach(Object entity) {
		try {
			getSession().evict( entity );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public FlushModeType getFlushMode() {
		FlushMode mode = getSession().getFlushMode();
		if ( mode == FlushMode.AUTO ) {
			this.flushModeType = FlushModeType.AUTO;
		}
		else if ( mode == FlushMode.COMMIT ) {
			this.flushModeType = FlushModeType.COMMIT;
		}
		else {
			return null; //TODO exception?
		}
		//otherwise this is an unknown mode for EJB3
		return flushModeType;
	}

	public void lock(Object entity, LockModeType lockMode) {
		try {
			if ( !isTransactionInProgress() ) {
				throw new TransactionRequiredException( "no transaction is in progress" );
			}
			//adjustFlushMode();
			if ( !contains( entity ) ) {
				throw new IllegalArgumentException( "entity not in the persistence context" );
			}
			getSession().lock( entity, getLockMode( lockMode ) );
		}
		catch ( HibernateException he ) {
			throwPersistenceException( he );
		}
	}

	public void lock(Object o, LockModeType lockModeType, Map<String, Object> stringObjectMap) {
		//FIXME
		//To change body of implemented methods use File | Settings | File Templates.
	}

	private LockMode getLockMode(LockModeType lockMode) {
		switch ( lockMode ) {
			case READ:
				return LockMode.UPGRADE; //assuming we are on read-commited and we need to prevent non repeteable read
			case WRITE:
				return LockMode.FORCE;
			default:
				throw new AssertionFailure( "Unknown LockModeType: " + lockMode );
		}
	}

	public boolean isTransactionInProgress() {
		return ( ( SessionImplementor ) getRawSession() ).isTransactionInProgress();
	}

	protected void markAsRollback() {
		log.debug( "mark transaction for rollback" );
		if ( tx.isActive() ) {
			tx.setRollbackOnly();
		}
		else {
			//no explicit use of the tx. boudaries methods
			if ( PersistenceUnitTransactionType.JTA == transactionType ) {
				TransactionManager transactionManager =
						( ( SessionFactoryImplementor ) getRawSession().getSessionFactory() ).getTransactionManager();
				if ( transactionManager == null ) {
					throw new PersistenceException(
							"Using a JTA persistence context wo setting hibernate.transaction.manager_lookup_class"
					);
				}
				try {
					transactionManager.setRollbackOnly();
				}
				catch ( SystemException e ) {
					throw new PersistenceException( "Unable to set the JTA transaction as RollbackOnly", e );
				}
			}
		}
	}

	public void joinTransaction() {
		joinTransaction( false );
	}

	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.equals( Session.class ) ) {
			return ( T ) getSession();
		}
		if ( clazz.equals( SessionImplementor.class ) ) {
			return ( T ) getSession();
		}
		//FIXME
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	private void joinTransaction(boolean ignoreNotJoining) {
		//set the joined status
		getSession().isOpen(); //for sync
		if ( transactionType == PersistenceUnitTransactionType.JTA ) {
			try {
				log.debug( "Looking for a JTA transaction to join" );
				final Session session = getSession();
				final Transaction transaction = session.getTransaction();
				if ( transaction != null && transaction instanceof JoinableCMTTransaction ) {
					//can't handle it if not a joinnable transaction
					final JoinableCMTTransaction joinableCMTTransaction = ( JoinableCMTTransaction ) transaction;

					if ( joinableCMTTransaction.getStatus() == JoinableCMTTransaction.JoinStatus.JOINED ) {
						log.debug( "Transaction already joined" );
						return; //no-op
					}
					joinableCMTTransaction.markForJoined();
					session.isOpen(); //register to the Tx
					if ( joinableCMTTransaction.getStatus() == JoinableCMTTransaction.JoinStatus.NOT_JOINED ) {
						if ( ignoreNotJoining ) {
							log.debug( "No JTA transaction found" );
							return;
						}
						else {
							throw new TransactionRequiredException(
									"No active JTA transaction on joinTransaction call"
							);
						}
					}
					else if ( joinableCMTTransaction.getStatus() == JoinableCMTTransaction.JoinStatus.MARKED_FOR_JOINED ) {
						throw new AssertionFailure( "Transaction MARKED_FOR_JOINED after isOpen() call" );
					}
					//flush before completion and
					//register clear on rollback
					log.trace( "Adding flush() and close() synchronization" );
					joinableCMTTransaction.registerSynchronization(
							new Synchronization() {
								public void beforeCompletion() {
									boolean flush = false;
									TransactionFactory.Context ctx = null;
									try {
										ctx = ( TransactionFactory.Context ) session;
										JoinableCMTTransaction joinable = ( JoinableCMTTransaction ) session.getTransaction();
										javax.transaction.Transaction transaction = joinable.getTransaction();
										if ( transaction == null ) {
											log.warn(
													"Transaction not available on beforeCompletionPhase: assuming valid"
											);
										}
										flush = !ctx.isFlushModeNever() &&
												//ctx.isFlushBeforeCompletionEnabled() &&
												//TODO probably make it ! isFlushBeforecompletion()
												( transaction == null || !JTAHelper.isRollback( transaction.getStatus() ) );
										//transaction == null workaround a JBoss TMBug
									}
									catch ( SystemException se ) {
										log.error( "could not determine transaction status", se );
										PersistenceException pe = new PersistenceException(
												"could not determine transaction status in beforeCompletion()",
												se
										);
										// handlePersistenceException will mark the transaction as rollbacked
										handlePersistenceException( pe );
										throw pe;
									}
									catch ( HibernateException he ) {
										throwPersistenceException( he );
									}

									try {
										if ( flush ) {
											log.trace( "automatically flushing session" );
											ctx.managedFlush();
										}
										else {
											log.trace( "skipping managed flushing" );
										}
									}
									catch ( HibernateException he ) {
										throw convert( he );
									}
									catch( PersistenceException pe ) {
										handlePersistenceException( pe );
										throw pe;
									}
									catch ( RuntimeException re ) {
										PersistenceException wrapped = new PersistenceException( re );
										handlePersistenceException( wrapped );
										throw wrapped;
									}
								}

								public void afterCompletion(int status) {
									try {
										if ( Status.STATUS_ROLLEDBACK == status
												&& transactionType == PersistenceUnitTransactionType.JTA ) {
											if ( session.isOpen() ) {
												session.clear();
											}
										}
										if ( session.isOpen() ) {
											//only reset if the session is opened since you can't get the Transaction otherwise
											JoinableCMTTransaction joinable = ( JoinableCMTTransaction ) session.getTransaction();
											joinable.resetStatus();
										}
									}
									catch ( HibernateException e ) {
										throw convert( e );
									}
								}
							}
					);
				}
				else {
					log.warn( "Cannot join transaction: do not override {}", Environment.TRANSACTION_STRATEGY );
				}
			}
			catch ( HibernateException he ) {
				throw convert( he );
			}
		}
		else {
			if ( !ignoreNotJoining ) {
				log.warn( "Calling joinTransaction() on a non JTA EntityManager" );
			}
		}
	}

	/**
	 * returns the underlying session
	 */
	public Object getDelegate() {
		return getSession();
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		tx = new TransactionImpl( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public void handlePersistenceException(PersistenceException e) {
		if ( e instanceof NoResultException ) {
			return;
		}
		if ( e instanceof NonUniqueResultException ) {
			return;
		}

		try {
			markAsRollback();
		}
		catch ( Exception ne ) {
			//we do not want the subsequent exception to swallow the original one
			log.error( "Unable to mark for rollback on PersistenceException: ", ne );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void throwPersistenceException(PersistenceException e) {
		handlePersistenceException( e );
		throw e;
	}

	/**
	 * {@inheritDoc}
	 */
	public RuntimeException convert(HibernateException e) {
		if ( e instanceof StaleStateException ) {
			PersistenceException converted = wrapStaleStateException( ( StaleStateException ) e );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof ObjectNotFoundException ) {
			EntityNotFoundException converted = new EntityNotFoundException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.NonUniqueResultException ) {
			NonUniqueResultException converted = new NonUniqueResultException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof UnresolvableObjectException ) {
			EntityNotFoundException converted = new EntityNotFoundException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof QueryException ) {
			return new IllegalArgumentException( e );
		}
		else if ( e instanceof TransientObjectException ) {
			try {
				markAsRollback();
			}
			catch ( Exception ne ) {
				//we do not want the subsequent exception to swallow the original one
				log.error( "Unable to mark for rollback on TransientObjectException: ", ne );
			}
			return new IllegalStateException( e ); //Spec 3.2.3 Synchronization rules
		}
		else {
			PersistenceException converted = new PersistenceException( e );
			handlePersistenceException( converted );
			return converted;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void throwPersistenceException(HibernateException e) {
		throw convert( e );
	}

	/**
	 * {@inheritDoc}
	 */
	public PersistenceException wrapStaleStateException(StaleStateException e) {
		PersistenceException pe;
		if ( e instanceof StaleObjectStateException ) {
			StaleObjectStateException sose = ( StaleObjectStateException ) e;
			Serializable identifier = sose.getIdentifier();
			if ( identifier != null ) {
				try {
					Object entity = getRawSession().load( sose.getEntityName(), identifier );
					if ( entity instanceof Serializable ) {
						//avoid some user errors regarding boundary crossing
						pe = new OptimisticLockException( null, e, entity );
					}
					else {
						pe = new OptimisticLockException( e );
					}
				}
				catch ( EntityNotFoundException enfe ) {
					pe = new OptimisticLockException( e );
				}
			}
			else {
				pe = new OptimisticLockException( e );
			}
		}
		else {
			pe = new OptimisticLockException( e );
		}
		return pe;
	}
}
