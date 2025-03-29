/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.spi;

/**
 * Common interface for all mappings that contain relational table information
 *
 * @author Steve Ebersole
 */
public interface TableInformationContainer {
	String getSchema();

	String getCatalog();

	String getTable();

	String getSubselect();

}
