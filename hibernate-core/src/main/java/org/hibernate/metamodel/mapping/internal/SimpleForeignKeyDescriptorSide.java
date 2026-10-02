package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

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

	@Nonnull
	@Override
	public ForeignKeyDescriptor.Nature getNature() {
		return nature;
	}

	@Nonnull
	@Override
	public BasicValuedModelPart getModelPart() {
		return modelPart;
	}
}
