/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

/**
 * Contract for entities (in the ERD sense) which can be exported via {@code CREATE}, {@code ALTER}, etc
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.tool.schema.spi.Exporter
 */
public interface Exportable {
	/**
	 * Get a unique identifier to make sure we are not exporting the same database structure multiple times.
	 *
	 * @return The exporting identifier.
	 */
	String getExportIdentifier();
}
