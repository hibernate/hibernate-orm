//$Id$
package org.hibernate.ejb;

import java.util.Map;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Interceptor;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gavin King
 */
public class EntityManagerImpl extends AbstractEntityManagerImpl {

	private static final Logger log = LoggerFactory.getLogger( EntityManagerImpl.class );
	protected Session session;
	protected SessionFactory sessionFactory;
	protected boolean open;
	protected boolean discardOnClose;
	private Class sessionInterceptorClass;

	public EntityManagerImpl(
			SessionFactory sessionFactory, PersistenceContextType pcType,
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose, Class sessionInterceptorClass, Map properties
	) {
		super( pcType, transactionType, properties );
		this.sessionFactory = sessionFactory;
		this.open = true;
		this.discardOnClose = discardOnClose;
		Object localSic = null;
		if (properties != null) localSic = properties.get( HibernatePersistence.SESSION_INTERCEPTOR );
		if ( localSic != null ) {
			if (localSic instanceof Class) {
				sessionInterceptorClass = (Class) localSic;
			}
			else if (localSic instanceof String) {
				try {
					sessionInterceptorClass =
							ReflectHelper.classForName( (String) localSic, EntityManagerImpl.class );
				}
				catch (ClassNotFoundException e) {
					throw new PersistenceException("Unable to instanciate interceptor: " + localSic, e);
				}
			}
			else {
				throw new PersistenceException("Unable to instanciate interceptor: " + localSic);
			}
		}
		this.sessionInterceptorClass = sessionInterceptorClass;
		postInit();
	}

	public Session getSession() {

		if ( !open ) throw new IllegalStateException( "EntityManager is closed" );
		return getRawSession();
	}

	protected Session getRawSession() {
		if ( session == null ) {
			Interceptor interceptor = null;
			if (sessionInterceptorClass != null) {
				try {
					interceptor = (Interceptor) sessionInterceptorClass.newInstance();
				}
				catch (InstantiationException e) {
					throw new PersistenceException("Unable to instanciate session interceptor: " + sessionInterceptorClass, e);
				}
				catch (IllegalAccessException e) {
					throw new PersistenceException("Unable to instanciate session interceptor: " + sessionInterceptorClass, e);
				}
				catch (ClassCastException e) {
					throw new PersistenceException("Session interceptor does not implement Interceptor: " + sessionInterceptorClass, e);
				}
			}
			session = sessionFactory.openSession( interceptor );
			if ( persistenceContextType == PersistenceContextType.TRANSACTION ) {
				( (SessionImplementor) session ).setAutoClear( true );
			}
		}
		return session;
	}

	public void close() {

		if ( !open ) throw new IllegalStateException( "EntityManager is closed" );
		if ( !discardOnClose && isTransactionInProgress() ) {
			//delay the closing till the end of the enlisted transaction
			getSession().getTransaction().registerSynchronization(
					new Synchronization() {
						public void beforeCompletion() {
							//nothing to do
						}

						public void afterCompletion(int i) {
							if ( session != null ) {
								if ( session.isOpen() ) {
									log.debug( "Closing entity manager after transaction completion" );
									session.close();
								}
								else {
									log.warn( "Entity Manager closed by someone else ({} must not be used)",
											Environment.AUTO_CLOSE_SESSION);
								}
							}
							//TODO session == null should not happen
						}
					}
			);
		}
		else {
			//close right now
			if ( session != null ) session.close();
		}
		open = false;
	}

	public boolean isOpen() {
		//adjustFlushMode(); //don't adjust, can't be done on closed EM
		try {
			if ( open ) getSession().isOpen(); //to force enlistment in tx
			return open;
		}
		catch (HibernateException he) {
			throwPersistenceException( he );
			return false;
		}
	}

}
