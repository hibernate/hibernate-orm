/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
