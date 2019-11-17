/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Simple implementation of FromClauseAccess
 *
 * @author Steve Ebersole
 */
public class SimpleFromClauseAccessImpl implements FromClauseAccess {
	protected final Map<NavigablePath, TableGroup> tableGroupMap = new HashMap<>();
	private final Map<NavigablePath, TableGroup> tableGroupMapNoAlias = new HashMap<>();

	public SimpleFromClauseAccessImpl() {
	}

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		TableGroup tableGroup = tableGroupMap.get( navigablePath );
		if ( tableGroup == null && !containsAlias( navigablePath ) ) {
			return tableGroupMapNoAlias.get( navigablePath );
		}
		return tableGroup;
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		final TableGroup previous = tableGroupMap.put( navigablePath, tableGroup );
		if ( containsAlias( navigablePath ) ) {
			tableGroupMapNoAlias.put( getPathWithoutAlias( navigablePath ), tableGroup );
		}
		if ( previous != null ) {
			SqlTreeCreationLogger.LOGGER.debugf(
					"Registration of TableGroup [%s] for NavigablePath [%s] overrode previous registration : %s",
					tableGroup,
					navigablePath,
					previous
			);
		}
	}

	protected boolean containsAlias(NavigablePath navigablePath) {
		return navigablePath.getLocalName().endsWith( ")" );
	}

	protected NavigablePath getPathWithoutAlias(NavigablePath navigablePath) {
		final String fullPath = navigablePath.getFullPath();
		final String navigableName = fullPath.substring( fullPath.lastIndexOf( '.' ) + 1, fullPath.lastIndexOf( '(' ) );
		return new NavigablePath( navigablePath.getParent(), navigableName );
	}
}
