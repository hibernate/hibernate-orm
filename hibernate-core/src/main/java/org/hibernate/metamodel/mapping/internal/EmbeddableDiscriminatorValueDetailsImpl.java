package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * Implementation of {@link DiscriminatorValueDetails} used for embeddable inheritance.
 *
 * @author Marco Belladelli
 * @see EmbeddableDiscriminatorConverter
 * @see EmbeddableDiscriminatorMapping
 */
public class EmbeddableDiscriminatorValueDetailsImpl implements DiscriminatorValueDetails {
	final Object value;
	final Class<?> embeddableClass;

	public EmbeddableDiscriminatorValueDetailsImpl(Object value, Class<?> embeddableClass) {
		this.value = value;
		this.embeddableClass = embeddableClass;
	}

	public Class<?> getEmbeddableClass() {
		return embeddableClass;
	}

	@Nullable
	@Override
	public Object getValue() {
		return value;
	}

	@Nonnull
	@Override
	public String getIndicatedEntityName() {
		return embeddableClass.getName();
	}

	@Nonnull
	@Override
	public EntityMappingType getIndicatedEntity() {
		throw new UnsupportedOperationException();
	}
}
