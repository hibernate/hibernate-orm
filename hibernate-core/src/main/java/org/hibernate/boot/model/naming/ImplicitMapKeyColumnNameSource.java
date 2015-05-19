/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name of a column used to back the key
 * of a {@link java.util.Map}.  This is used for both
 * {@link javax.persistence.MapKeyColumn} and
 * {@link javax.persistence.MapKeyJoinColumn} cases.
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.MapKeyColumn
 * @see javax.persistence.MapKeyJoinColumn
 */
public interface ImplicitMapKeyColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the AttributePath for the Map attribute
	 *
	 * @return The AttributePath for the Map attribute
	 */
	public AttributePath getPluralAttributePath();
}
