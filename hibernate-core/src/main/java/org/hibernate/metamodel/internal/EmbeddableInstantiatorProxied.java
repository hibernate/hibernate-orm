/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.function.Supplier;

import org.hibernate.bytecode.spi.BasicProxyFactory;
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
			Class<?> proxiedClass,
			Supplier<EmbeddableMappingType> embeddableMappingAccess,
			BasicProxyFactory factory) {
		this.proxiedClass = proxiedClass;
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.factory = factory;
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		final Object proxy = factory.getProxy();
		final var values = valuesAccess == null ? null : valuesAccess.getValues();
		if ( values != null ) {
			embeddableMappingAccess.get().setValues( proxy, values );
		}
		return proxy;
	}

	@Override
	public boolean isInstance(Object object) {
		return proxiedClass.isInstance( object );
	}

	@Override
	public boolean isSameClass(Object object) {
		return object.getClass() == proxiedClass;
	}
}
