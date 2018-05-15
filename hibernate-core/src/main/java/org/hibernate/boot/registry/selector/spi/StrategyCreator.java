/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.spi;

import java.util.function.Function;

/**
 * @deprecated (since 6.0) Just use {@link Function}
 */
@Deprecated
public interface StrategyCreator<T> extends Function<Class<? extends T>,T> {
	@Override
	default T apply(Class<? extends T> strategyImplClass) {
		return create( strategyImplClass );
	}

	<I extends T> I create(Class<I> strategyImplClass);
}
