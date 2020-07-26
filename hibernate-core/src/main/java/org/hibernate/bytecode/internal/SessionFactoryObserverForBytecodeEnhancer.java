/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		this.bytecodeProvider.resetCaches();
	}

	@Override
	public void sessionFactoryClosed(final SessionFactory factory) {
		this.bytecodeProvider.resetCaches();
	}
}
