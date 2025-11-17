/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * Composition of the aspects of column definition for standard "column types" exposed in XSD
 *
 * @author Steve Ebersole
 */
public interface JaxbColumnStandard
		extends JaxbColumn, JaxbColumnMutable, JaxbCheckable, JaxbColumnNullable, JaxbColumnUniqueable,
		JaxbColumnDefinable, JaxbColumnSizable, JaxbColumnDefaultable, JaxbCommentable {

	String getRead();
	String getWrite();
}
