/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
