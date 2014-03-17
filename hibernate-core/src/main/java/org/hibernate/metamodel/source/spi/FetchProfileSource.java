/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.spi;

/**
 * Defines a source of fetch profile information
 *
 * @author Steve Ebersole
 */
public interface FetchProfileSource {
	/**
	 * Defines a source of an association fetch information within a fetch profile
	 */
	public static interface AssociationOverrideSource {
		/**
		 * Retrieve the name of the entity containing the association.
		 *
		 * @return The entity name.
		 */
		public String getEntityName();

		/**
		 * Retrieve the name of the association attribute on the entity.
		 *
		 * @return The attribute name
		 */
		public String getAttributeName();

		/**
		 * Retrieve the name of the fetch mode to be applied to the association as part of this profile.
		 *
		 * @return the fetch mode name.
		 */
		public String getFetchModeName();
	}

	/**
	 * Retrieve the name of the profile.
	 *
	 * @return The profile name.
	 */
	public String getName();

	/**
	 * Retrieve the association fetching overrides associated with this profile.
	 * 
	 * @return The association fetching overrides
	 */
	public Iterable<AssociationOverrideSource> getAssociationOverrides();
}
