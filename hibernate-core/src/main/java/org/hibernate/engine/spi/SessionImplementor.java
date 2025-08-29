/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.descriptor.WrapperOptions;

import jakarta.persistence.criteria.CriteriaSelect;

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
	<T> QueryImplementor<T> createQuery(CriteriaSelect<T> selectQuery);

	/**
	 * Get the {@link ActionQueue} associated with this session.
	 */
	ActionQueue getActionQueue();

	@Override
	default TransactionCompletionCallbacks getTransactionCompletionCallbacks() {
		return getActionQueue();
	}

	@Override
	Object instantiate(EntityPersister persister, Object id) throws HibernateException;

	/**
	 * Initiate a flush to force deletion of a re-persisted entity.
	 */
	void forceFlush(EntityEntry e) throws HibernateException;
	/**
	 * Initiate a flush to force deletion of a re-persisted entity.
	 */
	void forceFlush(EntityKey e) throws HibernateException;

	@Override
	default <C> void runWithConnection(ConnectionConsumer<C> action) {
		doWork( connection -> {
			try {
				//noinspection unchecked
				action.accept( (C) connection );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );
	}

	@Override
	default <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
		return doReturningWork( connection -> {
			try {
				//noinspection unchecked
				return function.apply( (C) connection );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );
	}
}
