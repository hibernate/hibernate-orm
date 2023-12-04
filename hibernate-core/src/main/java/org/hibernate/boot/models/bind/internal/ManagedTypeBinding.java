/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.util.Map;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;

/**
 * Binding for an {@linkplain jakarta.persistence.metamodel.ManagedType managed type}
 *
 * @author Steve Ebersole
 */
public abstract class ManagedTypeBinding extends Binding {
	protected final ManagedTypeMetadata typeMetadata;

	public ManagedTypeBinding(
			ManagedTypeMetadata typeMetadata,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		super( bindingOptions, bindingState, bindingContext );
		this.typeMetadata = typeMetadata;
	}

	public ManagedTypeMetadata getTypeMetadata() {
		return typeMetadata;
	}

	public abstract Map<String, AttributeBinding> getAttributeBindings();
}
