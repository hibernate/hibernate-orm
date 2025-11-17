/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;

/**
 * @author Steve Ebersole
 */
public class SimpleForeignKeyDescriptorSide implements ForeignKeyDescriptor.Side {

	private final ForeignKeyDescriptor.Nature nature;
	private final BasicValuedModelPart modelPart;

	public SimpleForeignKeyDescriptorSide(
			ForeignKeyDescriptor.Nature nature,
			BasicValuedModelPart modelPart) {
		this.nature = nature;
		this.modelPart = modelPart;
	}

	@Override
	public ForeignKeyDescriptor.Nature getNature() {
		return nature;
	}

	@Override
	public BasicValuedModelPart getModelPart() {
		return modelPart;
	}
}
