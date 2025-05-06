/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools;

import java.util.Objects;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;


/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class EntityTools {
	public static boolean entitiesEqual(SessionImplementor session, String entityName, Object obj1, Object obj2) {
		final Object id1 = getIdentifier( session, entityName, obj1 );
		final Object id2 = getIdentifier( session, entityName, obj2 );

		return Objects.deepEquals( id1, id2 );
	}

	public static Object getIdentifier(SessionImplementor session, String entityName, Object obj) {
		if ( obj == null ) {
			return null;
		}

		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( obj );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}

		return session.getEntityPersister( entityName, obj ).getIdentifier( obj, session );
	}

	public static Object getTargetFromProxy(final SessionFactoryImplementor sessionFactoryImplementor, final LazyInitializer lazyInitializer) {
		if ( !lazyInitializer.isUninitialized() || activeProxySession( lazyInitializer ) ) {
			return lazyInitializer.getImplementation();
		}

		final SharedSessionContractImplementor sessionImplementor = lazyInitializer.getSession();
		final Session tempSession = sessionImplementor == null
				? sessionFactoryImplementor.openTemporarySession()
				: sessionImplementor.getFactory().openTemporarySession();
		try {
			return tempSession.get(
					lazyInitializer.getEntityName(),
					lazyInitializer.getInternalIdentifier()
			);
		}
		finally {
			tempSession.close();
		}
	}

	private static boolean activeProxySession(final LazyInitializer lazyInitializer) {
		final Session session = (Session) lazyInitializer.getSession();
		return session != null && session.isOpen() && session.isConnected();
	}

	/**
	 * @param clazz Class wrapped with a proxy or not.
	 * @param <T> Class type.
	 *
	 * @return Returns target class in case it has been wrapped with a proxy. If {@code null} reference is passed,
	 *         method returns {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getTargetClassIfProxied(Class<T> clazz) {
		if ( clazz == null ) {
			return null;
		}
		else if ( HibernateProxy.class.isAssignableFrom( clazz ) ) {
			// Get the source class of the proxy instance.
			return (Class<T>) clazz.getSuperclass();
		}
		return clazz;
	}

	/**
	 * @return Java class mapped to specified entity name.
	 */
	public static Class getEntityClass(SessionImplementor sessionImplementor, String entityName) {
		final EntityPersister entityPersister = sessionImplementor.getFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		return entityPersister.getMappedClass();
	}
}
