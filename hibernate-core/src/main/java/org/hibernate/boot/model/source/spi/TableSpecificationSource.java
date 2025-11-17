/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contract describing source of "table specification" information.
 *
 * @author Steve Ebersole
 */
public interface TableSpecificationSource {
	/**
	 * Obtain the supplied schema name
	 *
	 * @return The schema name. If {@code null}, the binder will apply the default.
	 */
	String getExplicitSchemaName();

	/**
	 * Obtain the supplied catalog name
	 *
	 * @return The catalog name. If {@code null}, the binder will apply the default.
	 */
	String getExplicitCatalogName();

	String getComment();

}
