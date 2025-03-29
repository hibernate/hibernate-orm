/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorConverter;
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

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public String getIndicatedEntityName() {
		return embeddableClass.getName();
	}

	@Override
	public EntityMappingType getIndicatedEntity() {
		throw new UnsupportedOperationException();
	}
}
