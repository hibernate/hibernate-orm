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

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import java.util.Map;

/**
 * Wrapper to manage the thread context ClassLoader before performing operations on an EntityMangerFactory instance.
 *
 * @author Bram Pouwelse
 */
public class EntityManagerFactoryTcclWrapper implements EntityManagerFactory {

	private final EntityManagerFactory m_delegate;
	
	private final ClassLoader m_classLoader;
	
	public EntityManagerFactoryTcclWrapper(EntityManagerFactory m_delegate, ClassLoader m_classLoader) {
		super();
		this.m_delegate = m_delegate;
		this.m_classLoader = m_classLoader;
	}

	public <T> void addNamedEntityGraph(String arg0, EntityGraph<T> arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_delegate.addNamedEntityGraph(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void addNamedQuery(String arg0, Query arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_delegate.addNamedQuery(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void close() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_delegate.close();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public EntityManager createEntityManager() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			EntityManager em = new EntityManagerTcclWrapper(m_delegate.createEntityManager(), m_classLoader);
			return em;

		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public EntityManager createEntityManager(@SuppressWarnings("rawtypes") Map arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			EntityManager em = new EntityManagerTcclWrapper(m_delegate.createEntityManager(arg0), m_classLoader);
			return em;
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public EntityManager createEntityManager(SynchronizationType arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			EntityManager em = new EntityManagerTcclWrapper(m_delegate.createEntityManager(arg0), m_classLoader);
			return em;
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}
	
	public EntityManager createEntityManager(SynchronizationType arg0, @SuppressWarnings("rawtypes") Map arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			EntityManager em = new EntityManagerTcclWrapper(m_delegate.createEntityManager(arg0, arg1), m_classLoader);
			return em;
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}


	public Cache getCache() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_delegate.getCache();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public CriteriaBuilder getCriteriaBuilder() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_delegate.getCriteriaBuilder();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Metamodel getMetamodel() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_delegate.getMetamodel();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public PersistenceUnitUtil getPersistenceUnitUtil() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_delegate.getPersistenceUnitUtil();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Map<String, Object> getProperties() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_delegate.getProperties();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public boolean isOpen() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_delegate.isOpen();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T unwrap(Class<T> arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_delegate.unwrap(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	} 
	
}
