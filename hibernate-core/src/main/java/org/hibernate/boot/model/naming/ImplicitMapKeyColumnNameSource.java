/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name of a column used to back the key
 * of a {@link java.util.Map}.  This is used for both
 * {@link jakarta.persistence.MapKeyColumn} and
 * {@link jakarta.persistence.MapKeyJoinColumn} cases.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.MapKeyColumn
 * @see jakarta.persistence.MapKeyJoinColumn
 */
public interface ImplicitMapKeyColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the AttributePath for the Map attribute
	 *
	 * @return The AttributePath for the Map attribute
	 */
	AttributePath getPluralAttributePath();
}
