/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
