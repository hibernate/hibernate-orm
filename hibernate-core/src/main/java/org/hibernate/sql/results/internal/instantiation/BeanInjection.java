/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.instantiation;

import org.hibernate.sql.results.spi.QueryResultAssembler;

/**
 * @author Steve Ebersole
 */
public class BeanInjection {
	private final BeanInjector beanInjector;
	private final QueryResultAssembler valueAssembler;

	public BeanInjection(BeanInjector beanInjector, QueryResultAssembler valueAssembler) {
		this.beanInjector = beanInjector;
		this.valueAssembler = valueAssembler;
	}

	public BeanInjector getBeanInjector() {
		return beanInjector;
	}

	public QueryResultAssembler getValueAssembler() {
		return valueAssembler;
	}
}
