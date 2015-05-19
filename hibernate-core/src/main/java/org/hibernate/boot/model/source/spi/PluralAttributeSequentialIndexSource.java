/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Defines the index of a persistent list/array
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public interface PluralAttributeSequentialIndexSource extends PluralAttributeIndexSource, RelationalValueSourceContainer {
	/**
	 * Hibernate allows specifying the base value to use when storing the index
	 * to the database.  This reports that "offset" value.
	 *
	 * @return The index base value.
	 */
	int getBase();
}
