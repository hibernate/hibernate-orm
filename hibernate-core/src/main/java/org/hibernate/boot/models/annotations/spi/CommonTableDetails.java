/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

/**
 * Information which is common across all table annotations
 *
 * @author Steve Ebersole
 */
public interface CommonTableDetails extends DatabaseObjectDetails, UniqueConstraintCollector, IndexCollector {
	/**
	 * The table name
	 */
	String name();

	/**
	 * Setter for {@linkplain #name()}
	 */
	void name(String name);
}
