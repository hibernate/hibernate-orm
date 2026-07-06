/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Delayed form of [SessionFactoryAccess] where the SessionFactory reference is injected
/// during SessionFactory construction.
///
/// @since 9.0
/// @author Steve Ebersole
public final class DelayedSessionFactoryReference implements SessionFactoryAccess {
	private volatile SessionFactoryImplementor sessionFactory;

	public void injectSessionFactory(SessionFactoryImplementor sessionFactory) {
		final var reference = Objects.requireNonNull( sessionFactory );
		if ( this.sessionFactory != null && this.sessionFactory != reference ) {
			throw new IllegalStateException( "SessionFactory reference has already been injected" );
		}
		this.sessionFactory = reference;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		final var reference = sessionFactory;
		if ( reference == null ) {
			throw new IllegalStateException( "SessionFactory reference is not yet available" );
		}
		return reference;
	}
}
