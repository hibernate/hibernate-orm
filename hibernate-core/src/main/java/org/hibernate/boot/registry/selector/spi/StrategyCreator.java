package org.hibernate.boot.registry.selector.spi;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface StrategyCreator<T> {
	T create(Class<? extends T> strategyClass);
}
