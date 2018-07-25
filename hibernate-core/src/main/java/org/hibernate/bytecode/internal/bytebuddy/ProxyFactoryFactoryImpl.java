/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

public class ProxyFactoryFactoryImpl implements ProxyFactoryFactory {

	private final ByteBuddyState byteBuddyState;

	private final ByteBuddyProxyHelper byteBuddyProxyHelper;

	public ProxyFactoryFactoryImpl(ByteBuddyState byteBuddyState, ByteBuddyProxyHelper byteBuddyProxyHelper) {
		this.byteBuddyState = byteBuddyState;
		this.byteBuddyProxyHelper = byteBuddyProxyHelper;
	}

	@Override
	public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
		return new ByteBuddyProxyFactory( byteBuddyProxyHelper );
	}

	@Override
	public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
		return new BasicProxyFactoryImpl( superClass, interfaces, byteBuddyState );
	}
}
