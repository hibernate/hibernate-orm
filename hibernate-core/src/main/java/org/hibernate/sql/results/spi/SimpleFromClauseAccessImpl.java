/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.SqlAstCreationLogger;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Simple implementation of FromClauseAccess
 *
 * @author Steve Ebersole
 */
public class SimpleFromClauseAccessImpl implements FromClauseAccess {
	protected final Map<NavigablePath, TableGroup> tableGroupMap = new HashMap<>();

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		return tableGroupMap.get( navigablePath );
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		final TableGroup previous = tableGroupMap.put( navigablePath, tableGroup );
		if ( previous != null ) {
			SqlAstCreationLogger.LOGGER.debugf(
					"Registration of TableGroup [%s] for NavigablePath [%s] overrode previous registration : %s",
					tableGroup,
					navigablePath,
					previous
			);
		}
	}

}
