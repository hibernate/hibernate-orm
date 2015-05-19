/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name related to basic values.
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.Column
 */
public interface ImplicitBasicColumnNameSource extends ImplicitNameSource {
	/**
	 * Access to the AttributePath for the basic value
	 *
	 * @return The AttributePath for the basic value
	 */
	public AttributePath getAttributePath();

	/**
	 * Is the basic column the "element column" for a collection?
	 * <p/>
	 * Historical handling for these in {@code hbm.xml} binding was to simply
	 * name the column "elt".
	 *
	 * @return {@code true} if the column being named is the collection element
	 * column; {@code false} otherwise.
	 */
	public boolean isCollectionElement();
}
