/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import java.util.List;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Provides access to information about existing index in the database
 *
 * @author Christoph Sturm
 * @author Steve Ebersole
 */
public interface IndexInformation {
	/**
	 * Obtain the identifier for this index.
	 *
	 * @return The index identifier.
	 */
	Identifier getIndexIdentifier();

	/**
	 * Obtain the columns indexed under this index.  Returned in sequential order.
	 *
	 * @return The columns
	 */
	List<ColumnInformation> getIndexedColumns();
}
