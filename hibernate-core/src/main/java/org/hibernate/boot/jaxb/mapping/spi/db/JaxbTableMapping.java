/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSchemaAware;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUniqueConstraintImpl;

/**
 * @author Steve Ebersole
 */
public interface JaxbTableMapping extends JaxbSchemaAware, JaxbCheckable, JaxbDatabaseObject {
	String getComment();
	String getOptions();

	List<JaxbIndexImpl> getIndexes();
	List<JaxbUniqueConstraintImpl> getUniqueConstraints();
}
