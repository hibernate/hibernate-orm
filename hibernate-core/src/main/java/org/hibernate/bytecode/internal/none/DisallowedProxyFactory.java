/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.none;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;

final class DisallowedProxyFactory implements ProxyFactory {

	static final DisallowedProxyFactory INSTANCE = new DisallowedProxyFactory();

	@Override
	public void postInstantiate(
			String entityName,
			Class persistentClass,
			Set<Class> interfaces,
			Method getIdentifierMethod,
			Method setIdentifierMethod,
			EmbeddedTypeDescriptor componentIdType) throws HibernateException {

	}

	@Override
	public HibernateProxy getProxy(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		throw new HibernateException( "Generation of HibernateProxy instances at runtime is not allowed when the configured BytecodeProvider is 'none'; your model requires a more advanced BytecodeProvider to be enabled." );
	}

}
