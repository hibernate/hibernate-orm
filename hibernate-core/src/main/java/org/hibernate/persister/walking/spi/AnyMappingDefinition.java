/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import org.hibernate.type.AnyType;
import org.hibernate.type.Type;

/**
 * Describes an ANY mapping
 *
 * @author Steve Ebersole
 */
public interface AnyMappingDefinition {
	/**
	 * Access to the mapping's AnyType
	 *
	 * @return The AnyType
	 */
	AnyType getType();

	/**
	 * Was the mapping defined as lazy?
	 *
	 * @return true/false
	 */
	boolean isLazy();

	/**
	 * Access to the type of the value that makes up the identifier portion of the AnyType.
	 *
	 * @return The identifier type
	 */
	Type getIdentifierType();

	/**
	 * Access to the type of the value that makes up the discriminator portion of the AnyType.  The discriminator is
	 * historically called the "meta".
	 * <p/>
	 * NOTE : If explicit discriminator mappings are given, the type here will be a {@link org.hibernate.type.MetaType}.
	 *
	 * @return The discriminator type
	 */
	Type getDiscriminatorType();

	/**
	 * Access to discriminator mappings explicitly defined in the mapping metadata.
	 *
	 * There are 2 flavors of discrimination:<ol>
	 *     <li>
	 *         The database holds the concrete entity names.  This is an implicit form, meaning that the discriminator
	 *         mappings do not have to be defined in the mapping metadata.  In this case, an empty iterable is returned
	 *         here
	 *     </li>
	 *     <li>
	 *         The database holds discriminator values that are interpreted to corresponding entity names based on
	 *         discriminator mappings explicitly supplied in the mapping metadata.  In this case, this method gives access
	 *         to those explicitly defined mappings.
	 *     </li>
	 * </ol>
	 *
	 * @return The explicitly defined discriminator value mappings.
	 */
	Iterable<DiscriminatorMapping> getMappingDefinedDiscriminatorMappings();

	/**
	 * Models a single discriminator mapping definition
	 */
	interface DiscriminatorMapping {
		/**
		 * Access to the defined discriminator value (the database value) being mapped.
		 *
		 * @return The defined discriminator value
		 */
		Object getDiscriminatorValue();

		/**
		 * Access to the defined entity name corresponding to the defined {@link #getDiscriminatorValue()}
		 *
		 * @return The defined entity name
		 */
		String getEntityName();
	}
}
