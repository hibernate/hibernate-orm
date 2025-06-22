/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;

/**
 * Composition of the aspects of column definition for join "column types" exposed in XSD
 *
 * @author Steve Ebersole
 */
public interface JaxbColumnJoined extends JaxbColumnCommon {
	String getReferencedColumnName();
	JaxbForeignKeyImpl getForeignKey();
}
