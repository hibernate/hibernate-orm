/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.none;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;

/**
 * When entities are enhanced in advance, proxies are not needed.
 */
final class NoProxyFactoryFactory implements ProxyFactoryFactory {

	@Override
	public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
		return DisallowedProxyFactory.INSTANCE;
	}

	@Override
	public BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface) {
		return new NoneBasicProxyFactory( superClassOrInterface );
	}
}
