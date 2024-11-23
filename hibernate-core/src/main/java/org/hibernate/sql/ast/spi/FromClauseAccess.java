/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.function.Function;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.tree.from.TableGroup;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Access to TableGroup indexing.  The indexing is defined in terms
 * of {@link NavigablePath}
 *
 * @author Steve Ebersole
 */
public interface FromClauseAccess {

	/**
	 * Find a TableGroup in this from clause without consulting parents by the NavigablePath it is registered under.
	 * Returns {@code null} if no TableGroup is registered under that NavigablePath
	 */
	TableGroup findTableGroupOnCurrentFromClause(NavigablePath navigablePath);

	/**
	 * Find a TableGroup by the NavigablePath it is registered under,
	 * and if not found on the current from clause level, ask the parent.  Returns
	 * {@code null} if no TableGroup is registered under that NavigablePath
	 */
	TableGroup findTableGroup(NavigablePath navigablePath);

	/**
	 * Find the TableGroup by the NavigablePath for the purpose of creating a
	 * new TableGroup if none can be found. Returns {@code null} if no TableGroup
	 * or parent table group is registered under that NavigablePath
	 */
	default TableGroup findTableGroupForGetOrCreate(NavigablePath navigablePath) {
		return findTableGroup( navigablePath );
	}

	/**
	 * Get a  TableGroup by the NavigablePath it is registered under.  If there is
	 * no registration, an exception is thrown.
	 */
	default TableGroup getTableGroup(NavigablePath navigablePath) throws SqlTreeCreationException {
		final TableGroup tableGroup = findTableGroup( navigablePath );
		if ( tableGroup == null ) {
			throw new SqlTreeCreationException( "Could not locate TableGroup - " + navigablePath );
		}
		return tableGroup;
	}

	/**
	 * Register a TableGroup under the given `navigablePath`.  Logs a message
	 * if this registration over-writes an existing one.
	 */
	void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup);

	/**
	 * Finds the TableGroup associated with the given `navigablePath`.  If one is not found,
	 * it is created via the given `creator`, registered under `navigablePath` and returned.
	 *
	 * @apiNote If the `creator` is called, there is no need for it to register the TableGroup
	 * it creates.  It will be registered by this method after.
	 *
	 * @see #findTableGroup
	 * @see #registerTableGroup
	 */
	default TableGroup resolveTableGroup(NavigablePath navigablePath, Function<NavigablePath, TableGroup> creator) {
		TableGroup tableGroup = findTableGroupForGetOrCreate( navigablePath );
		if ( tableGroup == null ) {
			tableGroup = creator.apply( navigablePath );
			registerTableGroup( navigablePath, tableGroup );
		}
		return tableGroup;
	}

	@Nullable TableGroup findTableGroupByIdentificationVariable(String identificationVariable);
}
