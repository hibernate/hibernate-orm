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
 * Holds per JVM JtaPlatform instance that may be updated with a new underlying Transaction Manager.
 *
 * @author Scott Marlow
 */
public class DefaultJtaPlatform implements JtaPlatformProvider {

	private static volatile JBossAppServerJtaPlatform delegate;

	static JBossAppServerJtaPlatform getDelegate() {
		return delegate;
	}

	static void setDelegate(JBossAppServerJtaPlatform delegate) {
		DefaultJtaPlatform.delegate = delegate;
	}

	@Override
	public JtaPlatform getProvidedJtaPlatform() {
		return delegate;
	}
}
