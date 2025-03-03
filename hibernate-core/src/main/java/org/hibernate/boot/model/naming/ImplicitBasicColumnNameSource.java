/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name related to basic values.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.Column
 */
public interface ImplicitBasicColumnNameSource extends ImplicitNameSource {
	/**
	 * Access to the AttributePath for the basic value
	 *
	 * @return The AttributePath for the basic value
	 */
	AttributePath getAttributePath();

	/**
	 * Is the basic column the "element column" for a collection?
	 * <p>
	 * Historical handling for these in {@code hbm.xml} binding was to simply
	 * name the column "elt".
	 *
	 * @return {@code true} if the column being named is the collection element
	 * column; {@code false} otherwise.
	 */
	boolean isCollectionElement();
}
