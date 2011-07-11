/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source;

import org.hibernate.cache.spi.access.AccessType;

/**
 * Defines a (contextual) set of values to use as defaults in the absence of related mapping information.  The
 * context here is conceptually a stack.  The "global" level is configuration settings.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface MappingDefaults {
	/**
	 * Identifies the default package name to use if none specified in the mapping.  Really only pertinent for
	 * {@code hbm.xml} mappings.
	 *
	 * @return The default package name.
	 */
	public String getPackageName();

	/**
	 * Identifies the default database schema name to use if none specified in the mapping.
	 *
	 * @return The default schema name
	 */
	public String getSchemaName();

	/**
	 * Identifies the default database catalog name to use if none specified in the mapping.
	 *
	 * @return The default catalog name
	 */
	public String getCatalogName();

	/**
	 * Identifies the default column name to use for the identifier column if none specified in the mapping.
	 *
	 * @return The default identifier column name
	 */
	public String getIdColumnName();

	/**
	 * Identifies the default column name to use for the discriminator column if none specified in the mapping.
	 *
	 * @return The default discriminator column name
	 */
	public String getDiscriminatorColumnName();

	/**
	 * Identifies the default cascade style to apply to associations if none specified in the mapping.
	 *
	 * @return The default cascade style
	 */
	public String getCascadeStyle();

	/**
	 * Identifies the default {@link org.hibernate.property.PropertyAccessor} name to use if none specified in the
	 * mapping.
	 *
	 * @return The default property accessor name
	 * @see org.hibernate.property.PropertyAccessorFactory
	 */
	public String getPropertyAccessorName();

	/**
	 * Identifies whether associations are lazy by default if not specified in the mapping.
	 *
	 * @return The default association laziness
	 */
	public boolean areAssociationsLazy();

	/**
	 * The default cache access type to use
	 *
	 * @return The default cache access type.
	 */
	public AccessType getCacheAccessType();
}
