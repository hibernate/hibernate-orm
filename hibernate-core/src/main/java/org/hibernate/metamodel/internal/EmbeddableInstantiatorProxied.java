/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;

/**
 * EmbeddableInstantiator used for instantiating "proxies" of an embeddable.
 */
public class EmbeddableInstantiatorProxied implements EmbeddableInstantiator {
	private final Class<?> proxiedClass;
	private final BasicProxyFactory factory;

	public EmbeddableInstantiatorProxied(Class proxiedClass, BasicProxyFactory factory) {
		this.proxiedClass = proxiedClass;
		this.factory = factory;
	}

	@Override
	public Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory) {
		return factory.getProxy();
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		return proxiedClass.isInstance( object );
	}

	@Override
	public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
		return object.getClass() == proxiedClass;
	}
}
