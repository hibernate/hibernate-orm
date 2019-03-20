/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import java.util.function.Function;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Access to TableGroup indexing.  The indexing is defined in terms
 * of {@link NavigablePath}
 *
 * @author Steve Ebersole
 */
public interface FromClauseAccess {
	/**
	 * Find a TableGroup by the NavigablePath it is registered under.  Returns
	 * {@code null} if no TableGroup is registered under that NavigablePath
	 */
	TableGroup findTableGroup(NavigablePath navigablePath);

	/**
	 * Get a  TableGroup by the NavigablePath it is registered under.  If there is
	 * no registration, an exception is thrown.
	 */
	default TableGroup getTableGroup(NavigablePath navigablePath) throws SqlTreeException {
		final TableGroup tableGroup = findTableGroup( navigablePath );
		if ( tableGroup == null ) {
			throw new SqlTreeException( "Could not locate TableGroup - " + navigablePath );
		}
		return tableGroup;
	}

	/**
	 * Register a TableGroup under the given `navigablePath`.  Logs a message
	 * if thhis registration over-writes an existing one.
	 */
	void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup);

	/**
	 * Finds the TableGroup associated with the given `navigablePath`.  If one is not found,
	 * it is created via the given ``, registered under `navigablePath` and returned.
	 *
	 * @apiNote If the `creator` is called, there is no need for it to register the TableGroup
	 * it creates.  It will be registered by this method after.
	 *
	 * @see #findTableGroup
	 * @see #registerTableGroup
	 */
	default TableGroup resolveTableGroup(NavigablePath navigablePath, Function<NavigablePath, TableGroup> creator) {
		TableGroup tableGroup = findTableGroup( navigablePath );
		if ( tableGroup == null ) {
			tableGroup = creator.apply( navigablePath );
			registerTableGroup( navigablePath, tableGroup );
		}
		return tableGroup;
	}
}
