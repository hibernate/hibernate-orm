/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

/**
 * Convenience base class for custom implementors of {@link SessionFactoryBuilderImplementor}, using delegation
 *
 * @author Guillaume Smet
 *
 * @param <T> The specific subclass; Allows subclasses to narrow the return type of the contract methods
 *            to a specialization of {@link MetadataBuilderImplementor}.
 */
public abstract class AbstractDelegatingSessionFactoryBuilderImplementor<T extends SessionFactoryBuilderImplementor>
		extends AbstractDelegatingSessionFactoryBuilder<T> implements SessionFactoryBuilderImplementor {

	public AbstractDelegatingSessionFactoryBuilderImplementor(SessionFactoryBuilderImplementor delegate) {
		super( delegate );
	}

	@Override
	protected SessionFactoryBuilderImplementor delegate() {
		return (SessionFactoryBuilderImplementor) super.delegate();
	}

	@Override
	public void disableJtaTransactionAccess() {
		delegate().disableJtaTransactionAccess();
	}

	@Override
	public SessionFactoryOptions buildSessionFactoryOptions() {
		return delegate().buildSessionFactoryOptions();
	}
}
