/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contact to define if the source of plural attribute is sortable or not.
 *
 * @author Steve Ebersole
 */
public interface Sortable {
	/**
	 * If the source of plural attribute is supposed to be sorted.
	 *
	 * @return <code>true</code> the attribute will be sortable or <code>false</code> means not.
	 */
	boolean isSorted();

	/**
	 * The comparator class name which will be used to sort the attribute.
	 *
	 * @return Qualified class name which implements {@link java.util.Comparator} contact.
	 */
	String getComparatorName();

}
