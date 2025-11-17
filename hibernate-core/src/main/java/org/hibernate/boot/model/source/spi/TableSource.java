/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contract describing source of table information
 *
 * @author Steve Ebersole
 */
public interface TableSource extends TableSpecificationSource {
	/**
	 * Obtain the supplied table name.
	 *
	 * @return The table name, or {@code null} is no name specified.
	 */
	String getExplicitTableName();

	String getRowId();

	String getCheckConstraint();
}
