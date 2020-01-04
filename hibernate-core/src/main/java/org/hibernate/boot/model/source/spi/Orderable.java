/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contact to define if a plural attribute source is orderable or not.
 *
 * @author Steve Ebersole
 */
public interface Orderable {
	/**
	 * If the source of plural attribute is supposed to be applied the {@code ORDER BY} when loading.
	 *
	 * @return {@code true} indicates to apply the {@code ORDER BY}; {@code false} means not.
	 */
	boolean isOrdered();

	/**
	 * The order by clause used during loading this plural attribute.
	 *
	 * <p/>
	 * If the ordering element is not specified, ordering by
	 * the primary key of the associated entity is assumed
	 *
	 * @see javax.persistence.OrderBy#value()
	 *
	 * @return The {@code ORDER BY} fragment used during loading this plural attribute from DB.
	 */
	String getOrder();
}
