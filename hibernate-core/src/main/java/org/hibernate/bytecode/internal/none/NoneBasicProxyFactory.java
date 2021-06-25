/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.none;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;

/**
 * This is used to instantiate "components" which are either Abstract or defined by an Interface.
 */
final class NoneBasicProxyFactory implements BasicProxyFactory {

	private final Class superClassOrInterface;

	@Deprecated
	public NoneBasicProxyFactory(Class superClass, Class[] interfaces) {
		if ( superClass == null && ( interfaces == null || interfaces.length == 0 ) ) {
			throw new AssertionFailure( "Attempting to build proxy without any superclass or interfaces" );
		}
		if ( superClass != null && ( interfaces != null && interfaces.length > 0 ) ) {
			throw new AssertionFailure( "Ambiguous call: this method can only be invoked with either a superClass or interfaces, not both" );
		}
		if ( interfaces != null && interfaces.length > 1 ) {
			throw new AssertionFailure( "Ambiguous call: this method can only accept a single interface, not multiple in the array (legacy expectation now being enforced)" );
		}
		this.superClassOrInterface = superClass != null ? superClass : interfaces[0];
	}

	public NoneBasicProxyFactory(Class superClassOrInterface) {
		this.superClassOrInterface = superClassOrInterface;
	}

	@Override
	public Object getProxy() {
		throw new HibernateException( "NoneBasicProxyFactory is unable to generate a BasicProxy for type " + superClassOrInterface + ". Enable a different BytecodeProvider." );
	}

}
