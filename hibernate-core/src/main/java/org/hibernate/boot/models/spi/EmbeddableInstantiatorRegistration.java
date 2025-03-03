/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.models.spi.ClassDetails;

/**
 * Registered {@linkplain org.hibernate.metamodel.spi.EmbeddableInstantiator}
 *
 * @see org.hibernate.annotations.EmbeddableInstantiatorRegistration
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorRegistration {
	private final org.hibernate.models.spi.ClassDetails embeddableClass;
	private final ClassDetails instantiator;

	public EmbeddableInstantiatorRegistration(ClassDetails embeddableClass, ClassDetails instantiator) {
		this.embeddableClass = embeddableClass;
		this.instantiator = instantiator;
	}

	public ClassDetails getEmbeddableClass() {
		return embeddableClass;
	}

	public ClassDetails getInstantiator() {
		return instantiator;
	}
}
