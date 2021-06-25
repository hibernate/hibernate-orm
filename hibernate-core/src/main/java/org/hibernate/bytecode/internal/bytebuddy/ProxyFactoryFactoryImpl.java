/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.AssertionFailure;
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
	@Deprecated
	public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
		if ( superClass == null && ( interfaces == null || interfaces.length == 0 ) ) {
			throw new AssertionFailure( "Attempting to build proxy without any superclass or interfaces" );
		}
		if ( superClass != null && ( interfaces != null && interfaces.length > 0 ) ) {
			throw new AssertionFailure( "Ambiguous call: this method can only be invoked with either a superClass or interfaces, not both" );
		}
		if ( interfaces != null && interfaces.length > 1 ) {
			throw new AssertionFailure( "Ambiguous call: this method can only accept a single interfaces, not multiple in the array (legacy expectation)" );
		}
		return buildBasicProxyFactory( superClass == null ? interfaces[0] : superClass );
	}

	public BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface) {
		if ( superClassOrInterface.isInterface() ) {
			return new BasicProxyFactoryImpl( null, superClassOrInterface, byteBuddyState );
		}
		else {
			return new BasicProxyFactoryImpl( superClassOrInterface, null, byteBuddyState );
		}
	}

}
