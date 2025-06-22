/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.none;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;

/**
 * This is used to instantiate "components" which are either Abstract or defined by an Interface.
 */
final class NoneBasicProxyFactory implements BasicProxyFactory {

	private final Class superClassOrInterface;

	public NoneBasicProxyFactory(Class superClassOrInterface) {
		this.superClassOrInterface = superClassOrInterface;
	}

	@Override
	public Object getProxy() {
		throw new HibernateException( "NoneBasicProxyFactory is unable to generate a BasicProxy for type " + superClassOrInterface + ". Enable a different BytecodeProvider." );
	}

}
