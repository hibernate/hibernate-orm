/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import java.util.function.Function;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public
interface FromClauseAccess {
	TableGroup findTableGroup(NavigablePath navigablePath);

	default TableGroup getTableGroup(NavigablePath navigablePath) {
		final TableGroup tableGroup = findTableGroup( navigablePath );
		if ( tableGroup == null ) {
			throw new SqlTreeException( "Could not locate TableGroup - " + navigablePath );
		}
		return tableGroup;
	}

	default void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		throw new NotYetImplementedFor6Exception();
	}

	default TableGroup resolveTableGroup(
			NavigablePath navigablePath,
			Function<NavigablePath, TableGroup> creator) {
		TableGroup tableGroup = findTableGroup( navigablePath );
		if ( tableGroup == null ) {
			tableGroup = creator.apply( navigablePath );
			registerTableGroup( navigablePath, tableGroup );
		}
		return tableGroup;
	}
}
