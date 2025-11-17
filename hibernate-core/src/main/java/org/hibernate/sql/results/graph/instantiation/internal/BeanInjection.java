/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import org.hibernate.sql.results.graph.DomainResultAssembler;

/**
 * @author Steve Ebersole
 */
public class BeanInjection {
	private final BeanInjector beanInjector;
	private final DomainResultAssembler valueAssembler;

	public BeanInjection(BeanInjector beanInjector, DomainResultAssembler valueAssembler) {
		this.beanInjector = beanInjector;
		this.valueAssembler = valueAssembler;
	}

	public BeanInjector getBeanInjector() {
		return beanInjector;
	}

	public DomainResultAssembler getValueAssembler() {
		return valueAssembler;
	}
}
