/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * Base definition for XSD column mappings
 *
 * @author Steve Ebersole
 */
public interface JaxbColumn extends JaxbDatabaseObject {
	String getName();

	default String getTable() {
		return null;
	}
}
