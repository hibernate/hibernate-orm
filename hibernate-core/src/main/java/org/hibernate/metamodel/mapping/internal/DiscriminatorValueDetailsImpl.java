package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorValueDetailsImpl implements DiscriminatorValueDetails {
	private final DiscriminatorValue value;
	private final EntityMappingType matchedEntityDescriptor;

	public DiscriminatorValueDetailsImpl(DiscriminatorValue value, EntityMappingType matchedEntityDescriptor) {
		this.value = value;
		this.matchedEntityDescriptor = matchedEntityDescriptor;
	}

	@Nullable
	@Override
	public Object getValue() {
		return value.value();
	}

	@Nonnull
	@Override
	public EntityMappingType getIndicatedEntity() {
		return matchedEntityDescriptor;
	}
}
