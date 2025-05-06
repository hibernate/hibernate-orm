/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	public BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface) {
		if ( superClassOrInterface.isInterface() ) {
			return new BasicProxyFactoryImpl( null, superClassOrInterface, byteBuddyState );
		}
		else {
			return new BasicProxyFactoryImpl( superClassOrInterface, null, byteBuddyState );
		}
	}

}
