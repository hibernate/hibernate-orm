/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import jakarta.persistence.DiscriminatorType;

import java.util.List;

/**
 * JAXB binding interface for discriminated association based attributes (any and many-to-any)
 *
 * @author Steve Ebersole
 */
public interface JaxbAnyMapping extends JaxbPersistentAttribute {
	/**
	 * Details about the logical association foreign-key
	 */
	Key getKey();

	/**
	 * Details about the discriminator
	 */
	Discriminator getDiscriminator();

	/**
	 * The key of a {@link JaxbAnyMapping} - the (logical) foreign-key value
	 *
	 * @author Steve Ebersole
	 */
	interface Key {
		List<JaxbColumnImpl> getColumns();
		String getType();
		String getJavaClass();
	}

	/**
	 * JAXB binding interface for describing the discriminator of a discriminated association
	 *
	 * @author Steve Ebersole
	 */
	interface Discriminator {
		/**
		 * The column holding the discriminator value
		 */
		JaxbColumnImpl getColumn();

		/**
		 * The type of discriminator
		 */
		DiscriminatorType getType();

		/**
		 * Mapping of discriminator-values to the corresponding entity names
		 */
		List<? extends JaxbDiscriminatorMapping> getValueMappings();
	}
}
