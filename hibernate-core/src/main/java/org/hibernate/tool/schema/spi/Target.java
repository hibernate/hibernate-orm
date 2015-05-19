/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

/**
 * Describes the target of schema management actions.  Typically this would be a stream/writer or the database
 * Connection
 *
 * @author Steve Ebersole
 */
public interface Target {
	/**
	 * Does this target accept actions coming from an import script?  If {@code false}, actions are not
	 * sent to this target's {@link #accept(String)} method
	 *
	 * @return {@code true} if import script actions should be sent to this target; {@code false} if they should not.
	 */
	public boolean acceptsImportScriptActions();

	/**
	 * Prepare for accepting actions
	 *
	 * @throws SchemaManagementException If there is a problem preparing the target.
	 */
	public void prepare();

	/**
	 * Accept a management action.  For stream/writer-based targets, this would indicate to write the action; for
	 * JDBC based targets, it would indicate to execute the action
	 *
	 * @param action The action to perform.
	 *
	 * @throws SchemaManagementException If there is a problem accepting the action.
	 */
	public void accept(String action);

	/**
	 * Release the target after all actions have been processed.
	 *
	 * @throws SchemaManagementException If there is a problem releasing the target.
	 */
	public void release();
}
