/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Base class for {@link SessionBuilderImplementor} implementations that wish to implement only parts of that contract
 * themselves while forwarding other method invocations to a delegate instance.
 *
 * @author Gunnar Morling
 */
public abstract class AbstractDelegatingSessionBuilderImplementor extends AbstractDelegatingSessionBuilder implements SessionBuilderImplementor {

	public AbstractDelegatingSessionBuilderImplementor(SessionBuilderImplementor delegate) {
		super( delegate );
	}

	protected SessionBuilderImplementor delegate() {
		return (SessionBuilderImplementor) super.delegate();
	}
}
