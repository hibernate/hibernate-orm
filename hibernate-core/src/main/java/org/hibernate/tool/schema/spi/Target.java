/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
