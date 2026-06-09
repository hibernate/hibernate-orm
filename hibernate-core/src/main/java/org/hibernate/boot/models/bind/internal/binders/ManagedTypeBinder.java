/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;

/// Base class for binders tied to one categorized managed type.
///
/// A managed-type binder owns the Hibernate Models metadata for one type plus the
/// shared binding context/state/options.  Subclasses decide which
/// [TypeBindingPhase] contracts they implement, making the coordinator's
/// phase dispatch explicit without forcing every binder to implement every phase.
///
/// @since 9.0
/// @author Steve Ebersole
public abstract class ManagedTypeBinder {
	private final ManagedTypeMetadata managedType;

	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext bindingContext;

	public ManagedTypeBinder(
			ManagedTypeMetadata managedType,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		this.managedType = managedType;
		this.state = state;
		this.options = options;
		this.bindingContext = bindingContext;
	}

	public ManagedTypeMetadata getManagedType() {
		return managedType;
	}

	public BindingState getBindingState() {
		return state;
	}

	public BindingOptions getOptions() {
		return options;
	}

	public BindingContext getBindingContext() {
		return bindingContext;
	}

	protected void prepareBinding(ModelBinders modelBinders) {
		// todo : ideally we'd pre-process attributes here, but the boot mapping model has
		//		no commonality between embeddable and identifiable (no ManagedType corollary)
	}
}
