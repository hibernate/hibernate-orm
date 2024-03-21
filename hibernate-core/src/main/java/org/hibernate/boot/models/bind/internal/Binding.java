/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
