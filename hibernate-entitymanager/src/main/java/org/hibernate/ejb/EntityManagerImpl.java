/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

import java.util.Map;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Hibernate implementation of {@link javax.persistence.EntityManager}.
 *
 * @author Gavin King
 */
public class EntityManagerImpl extends AbstractEntityManagerImpl implements SessionOwner {

    public static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class,
                                                                          EntityManagerImpl.class.getName());

	protected Session session;
	protected boolean open;
	protected boolean discardOnClose;
	private Class sessionInterceptorClass;

	public EntityManagerImpl(
			EntityManagerFactoryImpl entityManagerFactory,
			PersistenceContextType pcType,
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose,
			Class sessionInterceptorClass,
			Map properties) {
		super( entityManagerFactory, pcType, transactionType, properties );
		this.open = true;
		this.discardOnClose = discardOnClose;
		Object localSessionInterceptor = null;
		if (properties != null) {
			localSessionInterceptor = properties.get( AvailableSettings.SESSION_INTERCEPTOR );
		}
		if ( localSessionInterceptor != null ) {
			if (localSessionInterceptor instanceof Class) {
				sessionInterceptorClass = (Class) localSessionInterceptor;
			}
			else if (localSessionInterceptor instanceof String) {
				try {
					sessionInterceptorClass =
							ReflectHelper.classForName( (String) localSessionInterceptor, EntityManagerImpl.class );
				}
				catch (ClassNotFoundException e) {
					throw new PersistenceException("Unable to instanciate interceptor: " + localSessionInterceptor, e);
				}
			}
			else {
				throw new PersistenceException("Unable to instanciate interceptor: " + localSessionInterceptor);
			}
		}
		this.sessionInterceptorClass = sessionInterceptorClass;
		postInit();
	}

	@Override
    public Session getSession() {
		if ( !open ) {
			throw new IllegalStateException( "EntityManager is closed" );
		}
		return getRawSession();
	}

	@Override
    protected Session getRawSession() {
		if ( session == null ) {
			SessionBuilderImplementor sessionBuilder = getEntityManagerFactory().getSessionFactory().withOptions();
			sessionBuilder.owner( this );
			if (sessionInterceptorClass != null) {
				try {
					Interceptor interceptor = (Interceptor) sessionInterceptorClass.newInstance();
					sessionBuilder.interceptor( interceptor );
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
			sessionBuilder.autoJoinTransactions( getTransactionType() != PersistenceUnitTransactionType.JTA );
			session = sessionBuilder.openSession();
			if ( persistenceContextType == PersistenceContextType.TRANSACTION ) {
				( (SessionImplementor) session ).setAutoClear( true );
			}
		}
		return session;
	}

	public void close() {
		checkEntityManagerFactory();
		if ( !open ) {
			throw new IllegalStateException( "EntityManager is closed" );
		}
		if ( discardOnClose || !isTransactionInProgress() ) {
			//close right now
			if ( session != null ) {
				session.close();
			}
		}
		// Otherwise, session auto-close will be enabled by shouldAutoCloseSession().
		open = false;
	}

	public boolean isOpen() {
		//adjustFlushMode(); //don't adjust, can't be done on closed EM
		checkEntityManagerFactory();
		try {
			if ( open ) {
				getSession().isOpen(); //to force enlistment in tx
			}
			return open;
		}
		catch (HibernateException he) {
			throwPersistenceException( he );
			return false;
		}
	}

	@Override
	public boolean shouldAutoCloseSession() {
		return !isOpen();
	}

	private void checkEntityManagerFactory() {
		if (! getEntityManagerFactory().isOpen()) {
			open = false;
		}
	}
}
