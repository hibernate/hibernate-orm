package org.hibernate.boot.registry.selector.internal;

@FunctionalInterface
public interface LazyServiceResolver<T> {

	Class<? extends T> resolve(String name);

}
