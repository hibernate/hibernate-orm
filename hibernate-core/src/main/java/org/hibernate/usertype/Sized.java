/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;

import org.hibernate.engine.jdbc.Size;

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
