/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
