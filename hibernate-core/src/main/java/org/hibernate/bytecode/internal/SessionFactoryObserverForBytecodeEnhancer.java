/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.bytecode.spi.BytecodeProvider;

public final class SessionFactoryObserverForBytecodeEnhancer implements SessionFactoryObserver {

	private final BytecodeProvider bytecodeProvider;

	public SessionFactoryObserverForBytecodeEnhancer(BytecodeProvider bytecodeProvider) {
		this.bytecodeProvider = bytecodeProvider;
	}

	@Override
	public void sessionFactoryCreated(final SessionFactory factory) {
		this.bytecodeProvider.resetCaches();
	}

	@Override
	public void sessionFactoryClosing(final SessionFactory factory) {
		//unnecessary
	}

	@Override
	public void sessionFactoryClosed(final SessionFactory factory) {
		this.bytecodeProvider.resetCaches();
	}
}
