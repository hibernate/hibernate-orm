/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;

/**
 * @author Steve Ebersole
 */
public abstract class Binding {
	protected final BindingState bindingState;
	protected final BindingOptions bindingOptions;
	protected final BindingContext bindingContext;

	public Binding(BindingOptions bindingOptions, BindingState bindingState, BindingContext bindingContext) {
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	public abstract Object getBinding();
}
