package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;

/**
 * @author Steve Ebersole
 */
public class EmbeddedForeignKeyDescriptorSide<J, M, R> implements ForeignKeyDescriptor.Side {

	private final ForeignKeyDescriptor.Nature nature;
	private final EmbeddableValuedModelPart<J, M, R> modelPart;

	public EmbeddedForeignKeyDescriptorSide(
			ForeignKeyDescriptor.Nature nature,
			EmbeddableValuedModelPart<J, M, R> modelPart) {
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
	public EmbeddableValuedModelPart<J, M, R> getModelPart() {
		return modelPart;
	}
}
