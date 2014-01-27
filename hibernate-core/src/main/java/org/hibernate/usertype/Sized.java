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
package org.hibernate.usertype;

import org.hibernate.metamodel.spi.relational.Size;

/**
 * Extends dictated/default column size declarations from {@link org.hibernate.type.Type} to the {@link UserType}
 * hierarchy as well via an optional interface.
 *
 * @author Steve Ebersole
 */
public interface Sized {
	/**
	 * Return the column sizes dictated by this type.  For example, the mapping for a {@code char}/{@link Character} would
	 * have a dictated length limit of 1; for a string-based {@link java.util.UUID} would have a size limit of 36; etc.
	 *
	 * @todo Would be much much better to have this aware of Dialect once the service/metamodel split is done
	 *
	 * @return The dictated sizes.
	 *
	 * @see org.hibernate.type.Type#dictatedSizes
	 */
	public Size[] dictatedSizes();

	/**
	 * Defines the column sizes to use according to this type if the user did not explicitly say (and if no
	 * {@link #dictatedSizes} were given).
	 *
	 * @todo Would be much much better to have this aware of Dialect once the service/metamodel split is done
	 *
	 * @return The default sizes.
	 *
	 * @see org.hibernate.type.Type#defaultSizes
	 */
	public Size[] defaultSizes();
}
