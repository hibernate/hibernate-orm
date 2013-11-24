/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityGraph;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.SynchronizationType;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.ejb.AbstractEntityManagerImpl;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;

/**
 * Hibernate implementation of {@link javax.persistence.EntityManager}.
 *
 * @author Gavin King
 */
public class EntityManagerImpl extends AbstractEntityManagerImpl implements SessionOwner {

    public static final EntityManagerMessageLogger LOG = HEMLogging.messageLogger( EntityManagerImpl.class.getName() );

	protected Session session;
	protected boolean open;
	protected boolean discardOnClose;
	private Class sessionInterceptorClass;

	public EntityManagerImpl(
			EntityManagerFactoryImpl entityManagerFactory,
			PersistenceContextType pcType,
			SynchronizationType synchronizationType,
			PersistenceUnitTransactionType transactionType,
			boolean discardOnClose,
			Class sessionInterceptorClass,
			Map properties) {
		super( entityManagerFactory, pcType, synchronizationType, transactionType, properties );
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
	protected void checkOpen() {
		checkOpen( true );
	}

	@Override
	public void checkOpen(boolean markForRollbackIfClosed) {
		if( ! isOpen() ) {
			if ( markForRollbackIfClosed ) {
				markForRollbackOnly();
			}
			throw new IllegalStateException( "EntityManager is closed" );
		}
	}

	@Override
    public Session getSession() {
		checkOpen();
		return internalGetSession();
	}

	@Override
    protected Session getRawSession() {
		return internalGetSession();
	}

	@Override
	protected Session internalGetSession() {
		if ( session == null ) {
			SessionBuilderImplementor sessionBuilder = internalGetEntityManagerFactory().getSessionFactory().withOptions();
			sessionBuilder.owner( this );
			if (sessionInterceptorClass != null) {
				try {
					Interceptor interceptor = (Interceptor) sessionInterceptorClass.newInstance();
					sessionBuilder.interceptor( interceptor );
				}
				catch (InstantiationException e) {
					throw new PersistenceException("Unable to instantiate session interceptor: " + sessionInterceptorClass, e);
				}
				catch (IllegalAccessException e) {
					throw new PersistenceException("Unable to instantiate session interceptor: " + sessionInterceptorClass, e);
				}
				catch (ClassCastException e) {
					throw new PersistenceException("Session interceptor does not implement Interceptor: " + sessionInterceptorClass, e);
				}
			}
			sessionBuilder.autoJoinTransactions( getTransactionType() != PersistenceUnitTransactionType.JTA );
			session = sessionBuilder.openSession();
		}
		return session;
	}

	public void close() {
		checkEntityManagerFactory();
		checkOpen();

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
				internalGetSession().isOpen(); //to force enlistment in tx
			}
			return open;
		}
		catch (HibernateException he) {
			throwPersistenceException( he );
			return false;
		}
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		checkOpen();
		return new EntityGraphImpl<T>( null, getMetamodel().entity( rootType ), getEntityManagerFactory() );
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName) {
		checkOpen();
		final EntityGraphImpl named = getEntityManagerFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			return null;
		}
		return named.makeMutableCopy();
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityGraph<?> getEntityGraph(String graphName) {
		checkOpen();
		final EntityGraphImpl named = getEntityManagerFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			throw new IllegalArgumentException( "Could not locate EntityGraph with given name : " + graphName );
		}
		return named;
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		checkOpen();
		return getEntityManagerFactory().findEntityGraphsByType( entityClass );
	}

	@Override
	public boolean shouldAutoCloseSession() {
		return !isOpen();
	}

	private void checkEntityManagerFactory() {
		if ( ! internalGetEntityManagerFactory().isOpen() ) {
			open = false;
		}
	}
}
