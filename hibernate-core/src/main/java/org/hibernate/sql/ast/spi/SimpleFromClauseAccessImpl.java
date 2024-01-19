/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.tree.from.CorrelatedTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;

import org.jboss.logging.Logger;

/**
 * Simple implementation of FromClauseAccess
 *
 * @author Steve Ebersole
 */
public class SimpleFromClauseAccessImpl implements FromClauseAccess {

	protected final FromClauseAccess parent;
	protected final Map<NavigablePath, TableGroup> tableGroupMap = new HashMap<>();

	public SimpleFromClauseAccessImpl() {
		this( null );
	}

	public SimpleFromClauseAccessImpl(FromClauseAccess parent) {
		this.parent = parent;
	}

	@Override
	public TableGroup findTableGroupOnCurrentFromClause(NavigablePath navigablePath) {
		return tableGroupMap.get( navigablePath );
	}

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		final TableGroup tableGroup = tableGroupMap.get( navigablePath );
		if ( tableGroup != null || parent == null ) {
			return tableGroup;
		}
		return parent.findTableGroup( navigablePath );
	}

	@Override
	public TableGroup findTableGroupForGetOrCreate(NavigablePath navigablePath) {
		final TableGroup localTableGroup = tableGroupMap.get( navigablePath );
		if ( localTableGroup != null || parent == null ) {
			return localTableGroup;
		}
		else {
			final TableGroup tableGroup = parent.findTableGroup( navigablePath );
			if ( tableGroup != null && navigablePath.getParent() != null ) {
				final NavigableRole navigableRole = tableGroup.getModelPart().getNavigableRole();
				if ( navigableRole.getParent() != null ) {
					// Traverse up the navigable path to the point where resolving the path leads us to the parent TableGroup
					NavigableRole parentRole = navigableRole.getParent();
					NavigablePath parentPath = navigablePath.getParent();
					while ( parentRole.getParent() != null ) {
						parentRole = parentRole.getParent();
						parentPath = parentPath.getParent();
					}
					final TableGroup parentFound = parent.findTableGroup( parentPath );
					// Only return the TableGroup if there's no corresponding group in the parent FromClauseAccess
					// or if there is one, but it's the same or correlated to the locally found one.
					if ( parentFound == null || getCorrelatedTableGroup( parentFound ) == getCorrelatedTableGroup(
							tableGroupMap.get( parentPath ) ) ) {
						return tableGroup;
					}
					else {
						return null;
					}
				}
			}
			return tableGroup;
		}
	}

	public TableGroup findTableGroupForGetOrCreate(NavigablePath navigablePath, boolean allowLeftJoins) {
		final TableGroup tableGroup = findTableGroupForGetOrCreate( navigablePath );
		if ( !allowLeftJoins && tableGroup != null && navigablePath.getParent() != null
				&& CollectionPart.Nature.fromNameExact( navigablePath.getLocalName() ) == null ) {
			// This is an implicitly joined path, do not reuse existing table group if it's left joined
			final TableGroupJoin join = findTableGroup( navigablePath.getParent() ).findTableGroupJoin( tableGroup );
			return join != null && join.getJoinType() == SqlAstJoinType.LEFT ? null : tableGroup;
		}
		return tableGroup;
	}

	private TableGroup getCorrelatedTableGroup(TableGroup tableGroup) {
		if ( tableGroup instanceof CorrelatedTableGroup ) {
			return getCorrelatedTableGroup( ( (CorrelatedTableGroup) tableGroup ).getCorrelatedTableGroup() );
		}
		return tableGroup;
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		final Logger logger = SqlTreeCreationLogger.LOGGER;
		final boolean debugEnabled = logger.isDebugEnabled();
		if ( debugEnabled ) {
			logger.debugf(
					"Registration of TableGroup [%s] with identifierForTableGroup [%s] for NavigablePath [%s] ",
					tableGroup,
					tableGroup.getNavigablePath().getIdentifierForTableGroup(),
					navigablePath.getIdentifierForTableGroup()
			);
		}
		final TableGroup previous = tableGroupMap.put( navigablePath, tableGroup );
		if ( debugEnabled && previous != null ) {
			logger.debugf(
					"Registration of TableGroup [%s] for NavigablePath [%s] overrode previous registration : %s",
					tableGroup,
					navigablePath,
					previous
			);
		}
	}
}
