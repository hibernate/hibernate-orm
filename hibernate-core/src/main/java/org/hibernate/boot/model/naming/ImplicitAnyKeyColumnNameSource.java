/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name for an ANY mapping's key
 * column.  Historically the ANY key column name had to be specified.
 *
 * @author Steve Ebersole
 */
public interface ImplicitAnyKeyColumnNameSource extends ImplicitNameSource {
	/**
	 * Access to the AttributePath of the ANY mapping
	 *
	 * @return The AttributePath of the ANY mapping
	 */
	AttributePath getAttributePath();
}
