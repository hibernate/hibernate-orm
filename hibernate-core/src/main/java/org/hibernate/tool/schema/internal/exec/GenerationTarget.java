/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * Describes a schema generation target
 *
 * @author Steve Ebersole
 */
public interface GenerationTarget {
	/**
	 * Prepare for accepting actions
	 *
	 * @throws SchemaManagementException If there is a problem preparing the target.
	 */
	void prepare();

	/**
	 * Accept a command
	 *
	 * @param command The command
	 *
	 * @throws SchemaManagementException If there is a problem accepting the action.
	 */
	void accept(String command);

	/**
	 * Release this target, giving it a change to release its resources.
	 *
	 * @throws SchemaManagementException If there is a problem releasing the target.
	 */
	void release();
}
