/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Common interface for JAXB bindings that understand database schema (tables, sequences, etc).
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JaxbSchemaAware {
	String getSchema();
	void setSchema(String schema);

	String getCatalog();
	void setCatalog(String catalog);
}
