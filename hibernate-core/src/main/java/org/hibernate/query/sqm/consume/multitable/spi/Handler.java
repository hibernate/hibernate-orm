/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi;

/**
 * @author Steve Ebersole
 */
public interface Handler {
	/**
	 * Execute the multi-table update or delete indicated by the SQM AST
	 * passed in when this Handler was created.
	 *
	 * @param executionContext Contextual information needed for execution
	 *
	 * @return The "number of rows affected" count
	 */
	int execute(HandlerExecutionContext executionContext);
}
