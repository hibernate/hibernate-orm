package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
			@Nonnull Class<?> proxiedClass,
			@Nonnull Supplier<EmbeddableMappingType> embeddableMappingAccess,
			@Nonnull BasicProxyFactory factory) {
		this.proxiedClass = proxiedClass;
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.factory = factory;
	}

	@Override
	@Nonnull
	public Object instantiate(@Nonnull ValueAccess valuesAccess) {
		final Object proxy = factory.getProxy();
		final var values = valuesAccess == null ? null : valuesAccess.getValues();
		if ( values != null ) {
			embeddableMappingAccess.get().setValues( proxy, values );
		}
		return proxy;
	}

	@Override
	public boolean isInstance(@Nullable Object object) {
		return proxiedClass.isInstance( object );
	}

	@Override
	public boolean isSameClass(@Nonnull Object object) {
		return object.getClass() == proxiedClass;
	}
}
