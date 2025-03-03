/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface CollectionIdSource {
	/**
	 * Obtain source information about the column for the collection id.
	 *
	 * @return The collection id column info.
	 */
	ColumnSource getColumnSource();

	/**
	 * Obtain information about the Hibernate type ({@link org.hibernate.type.Type}) for the collection id
	 *
	 * @return The Hibernate type information
	 */
	HibernateTypeSource getTypeInformation();

	/**
	 * Obtain the name of the identifier value generator.
	 *
	 * @return The identifier value generator name
	 */
	String getGeneratorName();

	/**
	 * @return The identifier generator configuration parameters
	 */
	Map<String, String> getParameters();
}
