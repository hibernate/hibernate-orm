/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	public AnyType getType();

	/**
	 * Was the mapping defined as lazy?
	 *
	 * @return true/false
	 */
	public boolean isLazy();

	/**
	 * Access to the type of the value that makes up the identifier portion of the AnyType.
	 *
	 * @return The identifier type
	 *
	 * @see org.hibernate.annotations.AnyMetaDef#idType()
	 */
	public Type getIdentifierType();

	/**
	 * Access to the type of the value that makes up the discriminator portion of the AnyType.  The discriminator is
	 * historically called the "meta".
	 * <p/>
	 * NOTE : If explicit discriminator mappings are given, the type here will be a {@link org.hibernate.type.MetaType}.
	 *
	 * @return The discriminator type
	 *
	 * @see org.hibernate.annotations.Any#metaColumn()
	 * @see org.hibernate.annotations.AnyMetaDef#metaType()
	 */
	public Type getDiscriminatorType();

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
	 *         discriminator mappings explicitly supplied in the mapping metadata (see
	 *         {@link org.hibernate.annotations.AnyMetaDef#metaValues()}).  In this case, this method gives access
	 *         to those explicitly defined mappings.
	 *     </li>
	 * </ol>
	 *
	 * @return The explicitly defined discriminator value mappings.
	 */
	public Iterable<DiscriminatorMapping> getMappingDefinedDiscriminatorMappings();

	/**
	 * Models a single discriminator mapping definition
	 */
	public static interface DiscriminatorMapping {
		/**
		 * Access to the defined discriminator value (the database value) being mapped.
		 *
		 * @return The defined discriminator value
		 */
		public Object getDiscriminatorValue();

		/**
		 * Access to the defined entity name corresponding to the defined {@link #getDiscriminatorValue()}
		 *
		 * @return The defined entity name
		 */
		public String getEntityName();
	}
}
