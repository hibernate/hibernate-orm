/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping;

import java.util.List;

/**
 * JAXB binding interface for discriminated association based attributes (any and many-to-any)
 *
 * @author Steve Ebersole
 */
public interface DiscriminatedAssociation extends PersistentAttribute {
	/**
	 * Details about the logical association foreign-key
	 */
	Key getKey();

	/**
	 * Details about the discriminator
	 */
	Discriminator getDiscriminator();

	/**
	 * The key of a {@link DiscriminatedAssociation} - the (logical) foreign-key value
	 *
	 * @author Steve Ebersole
	 */
	interface Key {
		List<JaxbColumn> getColumns();
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
		JaxbColumn getColumn();

		/**
		 * Mapping of discriminator-values to the corresponding entity names
		 */
		List<? extends DiscriminatorMapping> getValueMappings();
	}
}
