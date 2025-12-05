/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contains the state necessary to properly implement SessionAssociableInterceptor;
 * this allows reuse of this particular object across multiple interceptor instances
 * and keeps them from getting too allocation-intensive.
 * It also allows for a very elegant, simple way for the Session to clear references
 * to itself on close.
 */
public final class SessionAssociationMarkers {

	private static final SessionAssociationMarkers NON_ASSOCIATED = new SessionAssociationMarkers();

	final boolean allowLoadOutsideTransaction;
	final String sessionFactoryUuid;
	transient SharedSessionContractImplementor session; //TODO by resetting this one field on Session#close we might be able to avoid iterating on all managed instances to de-associate them? Only if we guarantee all managed types use this.

	public SessionAssociationMarkers(final SharedSessionContractImplementor session) {
		this.session = session;
		allowLoadOutsideTransaction =
				session.getFactory().getSessionFactoryOptions()
						.isInitializeLazyStateOutsideTransactionsEnabled();
		sessionFactoryUuid = allowLoadOutsideTransaction ? session.getFactory().getUuid() : null;
	}

	/**
	 * Constructor for the singleton representing non-associated
	 * state.
	 */
	private SessionAssociationMarkers() {
		session = null;
		sessionFactoryUuid = null;
		allowLoadOutsideTransaction = false;
	}

	/**
	 * Copying constructor for when we're allowed to load outside of transactions
	 * and need to transparently reassociated to the SessionFactory having the
	 * specified UUID.
	 *
	 * @param uuid The UUID of the SessionFactory
	 */
	private SessionAssociationMarkers(String uuid) {
		session = null;
		sessionFactoryUuid = uuid;
		allowLoadOutsideTransaction = true;
	}

	public SessionAssociationMarkers deAssociatedCopy() {
		return allowLoadOutsideTransaction
				? new SessionAssociationMarkers( sessionFactoryUuid )
				: NON_ASSOCIATED;
	}

	/**
	 * Careful as this mutates the state of this instance, which is possibly
	 * used by multiple managed entities.
	 * Removes the reference to the session; useful on Session close.
	 */
	public void sessionClosed() {
		session = null;
	}

}
