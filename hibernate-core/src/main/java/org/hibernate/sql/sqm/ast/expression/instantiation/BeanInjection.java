/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression.instantiation;

import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;

/**
 * @author Steve Ebersole
 */
public class BeanInjection<T,V> {
	private final BeanInjector<T> beanInjector;
	private final ReturnReader<V> valueReader;

	public BeanInjection(BeanInjector<T> beanInjector, ReturnReader<V> valueReader) {
		this.beanInjector = beanInjector;
		this.valueReader = valueReader;
	}

	public BeanInjector<T> getBeanInjector() {
		return beanInjector;
	}

	public ReturnReader<V> getValueReader() {
		return valueReader;
	}
}
