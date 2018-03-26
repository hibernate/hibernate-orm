/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.jboss.as.jpa.hibernate5;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformProvider;

/**
 * @author Scott Marlow
 */
public class JtaPlatformProviderImpl implements JtaPlatformProvider {

	private static volatile JBossAppServerJtaPlatform delegate;

	public static JBossAppServerJtaPlatform getDelegate() {
		return delegate;
	}

	public static void setDelegate(JBossAppServerJtaPlatform delegate) {
		JtaPlatformProviderImpl.delegate = delegate;
	}

	@Override
	public JtaPlatform getProvidedJtaPlatform() {
		return delegate;
	}
}
