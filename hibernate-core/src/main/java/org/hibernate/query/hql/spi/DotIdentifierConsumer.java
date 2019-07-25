/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
	 * Get the currently consumed part.  Generally called after the whole path
	 * has been processed at which point this will return the final outcome of the
	 * consumption
	 */
	SemanticPathPart getConsumedPart();
}
