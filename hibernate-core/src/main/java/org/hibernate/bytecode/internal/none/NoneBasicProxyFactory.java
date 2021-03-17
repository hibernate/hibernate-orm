/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.none;

import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;

/**
 * This is used to instantiate "components" which are either Abstract or defined by an Interface.
 */
final class NoneBasicProxyFactory implements BasicProxyFactory {

	private final Class superClass;
	private final Class[] interfaces;

	public NoneBasicProxyFactory(Class superClass, Class[] interfaces) {
		this.superClass = superClass;
		this.interfaces = interfaces;
	}

	@Override
	public Object getProxy() {
		throw new HibernateException( "NoneBasicProxyFactory is unable to generate a BasicProxy for type " + superClass + " and interfaces " + Arrays.toString( interfaces ) + ". Enable a different BytecodeProvider." );
	}

}
