/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Defines the "internal contract" between {@link Session} and other parts of Hibernate
 * including implementors of {@link org.hibernate.type.Type}, {@link EntityPersister},
 * and {@link org.hibernate.persister.collection.CollectionPersister}.
 * <p>
 * The {@code Session}, via this interface and {@link SharedSessionContractImplementor},
 * implements:
 * <ul>
 *     <li>
 *         {@link JdbcSessionOwner}, and so the session also acts as the orchestrator
 *         of a "JDBC session", and may be used to construct a {@link JdbcCoordinator}.
 *     </li>
 *     <li>
 *         {@link TransactionCoordinatorBuilder.Options}, allowing the session to control
 *         the creation of the {@link TransactionCoordinator} delegate when it is passed
 *         as an argument to
 *         {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder#buildTransactionCoordinator}
 *     </li>
 *     <li>
 *         {@link LobCreationContext}, and so the session may act as the context for
 *         JDBC LOB instance creation.
 *     </li>
 *     <li>
 *         {@link WrapperOptions}, and so the session may influence the process of binding
 *         and extracting values to and from JDBC, which is performed by implementors of
 *         {@link org.hibernate.type.descriptor.jdbc.JdbcType}.
 *     </li>
 * </ul>
 *
 * See also {@link org.hibernate.event.spi.EventSource} which extends this interface,
 * providing a bridge to the event generation features of {@link org.hibernate.event.spi
 * org.hibernate.event}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionImplementor extends Session, SharedSessionContractImplementor {

	@Override
	default SessionImplementor getSession() {
		return this;
	}

	@Override
	SessionFactoryImplementor getSessionFactory();

	@Override
	<T> RootGraphImplementor<T> createEntityGraph(Class<T> rootType);

	@Override
	RootGraphImplementor<?> createEntityGraph(String graphName);

	@Override
	RootGraphImplementor<?> getEntityGraph(String graphName);

	/**
	 * Get the {@link ActionQueue} associated with this session.
	 */
	ActionQueue getActionQueue();

	Object instantiate(EntityPersister persister, Object id) throws HibernateException;

	/**
	 * Initiate a flush to force deletion of a re-persisted entity.
	 */
	void forceFlush(EntityEntry e) throws HibernateException;
	/**
	 * Initiate a flush to force deletion of a re-persisted entity.
	 */
	void forceFlush(EntityKey e) throws HibernateException;

	/**
	 * Cascade the lock operation to the given child entity.
	 */
	void lock(String entityName, Object child, LockOptions lockOptions);

	/**
	 * @deprecated  OperationalContext should cover this overload I believe
	 */
	@Deprecated
	void merge(String entityName, Object object, MergeContext copiedAlready) throws HibernateException;

	/**
	 * @deprecated  OperationalContext should cover this overload I believe
	 */
	@Deprecated
	void persist(String entityName, Object object, PersistContext createdAlready) throws HibernateException;

	/**
	 * @deprecated  OperationalContext should cover this overload I believe
	 */
	@Deprecated
	void persistOnFlush(String entityName, Object object, PersistContext copiedAlready);

	/**
	 * @deprecated  OperationalContext should cover this overload I believe
	 */
	@Deprecated
	void refresh(String entityName, Object object, RefreshContext refreshedAlready) throws HibernateException;

	/**
	 * @deprecated  OperationalContext should cover this overload I believe
	 */
	@Deprecated
	void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, DeleteContext transientEntities);

	/**
	 * @deprecated  OperationalContext should cover this overload I believe
	 */
	@Deprecated
	void removeOrphanBeforeUpdates(String entityName, Object child);

	@Override
	default SessionImplementor asSessionImplementor() {
		return this;
	}

	@Override
	default boolean isSessionImplementor() {
		return true;
	}

}
