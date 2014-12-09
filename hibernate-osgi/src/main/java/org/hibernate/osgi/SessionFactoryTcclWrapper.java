/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.osgi;

import org.hibernate.*;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper to manage the thread context ClassLoader before performing operations on an SessionFactory instance.
 *
 * @author Bram Pouwelse
 */
public class SessionFactoryTcclWrapper implements SessionFactory {

	private final SessionFactory sessionFactory;

	private final ClassLoader classLoader;

	public SessionFactoryTcclWrapper(SessionFactory sessionFactory, ClassLoader classLoader){
		this.sessionFactory = sessionFactory;
		this.classLoader = classLoader;
	}


	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getSessionFactoryOptions();
		}finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public SessionBuilder withOptions() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.withOptions();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Session openSession() throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.openSession();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Session getCurrentSession() throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getCurrentSession();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public StatelessSessionBuilder withStatelessOptions() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.withStatelessOptions();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public StatelessSession openStatelessSession() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.openStatelessSession();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public StatelessSession openStatelessSession(Connection connection) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.openStatelessSession(connection);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public ClassMetadata getClassMetadata(Class entityClass) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getClassMetadata(entityClass);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public ClassMetadata getClassMetadata(String entityName) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getClassMetadata(entityName);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public CollectionMetadata getCollectionMetadata(String roleName) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getCollectionMetadata(roleName);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Map<String, ClassMetadata> getAllClassMetadata() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getAllClassMetadata();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Map getAllCollectionMetadata() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getAllCollectionMetadata();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Statistics getStatistics() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getStatistics();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public void close() throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.close();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public boolean isClosed() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.isClosed();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Cache getCache() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getCache();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evict(Class persistentClass) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evict(persistentClass);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evict(Class persistentClass, Serializable id) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evict(persistentClass, id);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evictEntity(String entityName) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evictEntity(entityName);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evictEntity(String entityName, Serializable id) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evictEntity(entityName, id);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evictCollection(String roleName) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evictCollection(roleName);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evictCollection(String roleName, Serializable id) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evictCollection(roleName, id);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evictQueries(String cacheRegion) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evictQueries(cacheRegion);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	@Deprecated
	public void evictQueries() throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			sessionFactory.evictQueries();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Set getDefinedFilterNames() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getDefinedFilterNames();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getFilterDefinition(filterName);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public boolean containsFetchProfileDefinition(String name) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.containsFetchProfileDefinition(name);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public TypeHelper getTypeHelper() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getTypeHelper();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public Reference getReference() throws NamingException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			return sessionFactory.getReference();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}

	}
}
