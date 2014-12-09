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
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import java.util.List;
import java.util.Map;

/**
 * Wrapper to manage the thread context ClassLoader before performing operations on an EntityManger instance.
 *
 * @author Bram Pouwelse
 */
public class EntityManagerTcclWrapper implements EntityManager {

	private final EntityManager m_entityManager;
	
	private final ClassLoader m_classLoader;
	
	public EntityManagerTcclWrapper(EntityManager m_entityManager, ClassLoader m_classLoader) {
		this.m_entityManager = m_entityManager;
		this.m_classLoader = m_classLoader;
	}

	public void clear() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);
			
			m_entityManager.clear();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void close() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);
			
			m_entityManager.close();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public boolean contains(Object arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);
			
			return m_entityManager.contains(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> EntityGraph<T> createEntityGraph(Class<T> arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);
			
			return m_entityManager.createEntityGraph(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public EntityGraph<?> createEntityGraph(String arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createEntityGraph(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createNamedQuery(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Query createNamedQuery(String arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createNamedQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public StoredProcedureQuery createNamedStoredProcedureQuery(String arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createNamedStoredProcedureQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Query createNativeQuery(String arg0, @SuppressWarnings("rawtypes") Class arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createNativeQuery(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Query createNativeQuery(String arg0, String arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createNativeQuery(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Query createNativeQuery(String arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createNativeQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);
	
			return m_entityManager.createQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createQuery(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Query createQuery(String arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0,
			@SuppressWarnings("rawtypes") Class... arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createStoredProcedureQuery(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0,
			String... arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createStoredProcedureQuery(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.createStoredProcedureQuery(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void detach(Object arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.detach(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2,
			Map<String, Object> arg3) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.find(arg0, arg1, arg2, arg3);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.find(arg0, arg1, arg2);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.find(arg0, arg1, arg2);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T find(Class<T> arg0, Object arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.find(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void flush() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.flush();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public CriteriaBuilder getCriteriaBuilder() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getCriteriaBuilder();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Object getDelegate() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getDelegate();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public EntityGraph<?> getEntityGraph(String arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getEntityGraph(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getEntityGraphs(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public EntityManagerFactory getEntityManagerFactory() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getEntityManagerFactory();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public FlushModeType getFlushMode() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getFlushMode();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public LockModeType getLockMode(Object arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getLockMode(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Metamodel getMetamodel() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getMetamodel();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public Map<String, Object> getProperties() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getProperties();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T getReference(Class<T> arg0, Object arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getReference(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public EntityTransaction getTransaction() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.getTransaction();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public boolean isJoinedToTransaction() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.isJoinedToTransaction();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public boolean isOpen() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.isOpen();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void joinTransaction() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.joinTransaction();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.lock(arg0, arg1, arg2);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void lock(Object arg0, LockModeType arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.lock(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T merge(T arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.merge(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void persist(Object arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.persist(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.refresh(arg0, arg1, arg2);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void refresh(Object arg0, LockModeType arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.refresh(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void refresh(Object arg0, Map<String, Object> arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.refresh(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void refresh(Object arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.refresh(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void remove(Object arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.remove(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void setFlushMode(FlushModeType arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.setFlushMode(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public void setProperty(String arg0, Object arg1) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			m_entityManager.setProperty(arg0, arg1);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	public <T> T unwrap(Class<T> arg0) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(m_classLoader);

			return m_entityManager.unwrap(arg0);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}
	
}
