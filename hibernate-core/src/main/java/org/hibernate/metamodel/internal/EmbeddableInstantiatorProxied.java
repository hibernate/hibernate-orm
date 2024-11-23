/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * EmbeddableInstantiator used for instantiating "proxies" of an embeddable.
 */
public class EmbeddableInstantiatorProxied implements StandardEmbeddableInstantiator {
	private final Supplier<EmbeddableMappingType> embeddableMappingAccess;
	private final Class<?> proxiedClass;
	private final BasicProxyFactory factory;

	public EmbeddableInstantiatorProxied(
			Class proxiedClass,
			Supplier<EmbeddableMappingType> embeddableMappingAccess, BasicProxyFactory factory) {
		this.proxiedClass = proxiedClass;
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.factory = factory;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess, SessionFactoryImplementor sessionFactory) {
		final Object proxy = factory.getProxy();
		Object[] values = valuesAccess == null ? null : valuesAccess.getValues();
		if ( values != null ) {
			final EmbeddableMappingType embeddableMapping = embeddableMappingAccess.get();
			embeddableMapping.setValues( proxy, values );
		}
		return proxy;
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
