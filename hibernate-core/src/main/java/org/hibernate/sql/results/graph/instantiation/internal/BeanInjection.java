package org.hibernate.sql.results.graph.instantiation.internal;

import org.hibernate.sql.results.graph.DomainResultAssembler;

/**
 * @author Steve Ebersole
 */
public class BeanInjection<T> {
	private final BeanInjector<T> beanInjector;
	private final DomainResultAssembler<?> valueAssembler;

	public BeanInjection(BeanInjector<T> beanInjector, DomainResultAssembler<?> valueAssembler) {
		this.beanInjector = beanInjector;
		this.valueAssembler = valueAssembler;
	}

	public BeanInjector<T> getBeanInjector() {
		return beanInjector;
	}

	public DomainResultAssembler<?> getValueAssembler() {
		return valueAssembler;
	}
}
