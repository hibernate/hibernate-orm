/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.spi;

/**
 * Consumes the parts of a path.
 *
 * @author Steve Ebersole
 */
public interface DotIdentifierConsumer {
	/**
	 * Responsible for consuming each part of the path.  Called sequentially for
	 * each part.
	 *
	 * @param identifier The current part of the path being processed
	 * @param isBase Is this the base of the path (the first token)?
	 * @param isTerminal Is this the terminus of the path (last token)?
	 */
	void consumeIdentifier(String identifier, boolean isBase, boolean isTerminal);

	/**
	 * Responsible for consuming each part of the path.  Called sequentially for
	 * each part.
	 *
	 * @param entityName The treat target entity name
	 * @param isTerminal Is this the terminus of the path (last token)?
	 */
	void consumeTreat(String entityName, boolean isTerminal);

	/**
	 * Get the currently consumed part.  Generally called after the whole path
	 * has been processed at which point this will return the final outcome of the
	 * consumption
	 */
	SemanticPathPart getConsumedPart();
}
