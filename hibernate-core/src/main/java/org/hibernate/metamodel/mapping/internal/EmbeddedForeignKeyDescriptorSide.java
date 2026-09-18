/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;

/**
 * @author Steve Ebersole
 */
public class EmbeddedForeignKeyDescriptorSide implements ForeignKeyDescriptor.Side {

	private final ForeignKeyDescriptor.Nature nature;
	private final EmbeddableValuedModelPart modelPart;

	public EmbeddedForeignKeyDescriptorSide(
			ForeignKeyDescriptor.Nature nature,
			EmbeddableValuedModelPart modelPart) {
		this.nature = nature;
		this.modelPart = modelPart;
	}

	@Nonnull
	@Override
	public ForeignKeyDescriptor.Nature getNature() {
		return nature;
	}

	@Nonnull
	@Override
	public EmbeddableValuedModelPart getModelPart() {
		return modelPart;
	}
}
