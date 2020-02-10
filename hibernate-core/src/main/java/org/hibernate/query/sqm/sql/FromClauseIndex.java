/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.sqm.sql;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.tree.from.TableGroup;

import org.jboss.logging.Logger;

/**
 * An index of various FROM CLAUSE resolutions.
 *
 * @author Steve Ebersole
 */
public class FromClauseIndex extends SimpleFromClauseAccessImpl {
	private static final Logger log = Logger.getLogger( FromClauseIndex.class );

	private final Map<String, TableGroup> tableGroupByAliasXref = new HashMap<>();

	/**
	 * Holds *explicitly* fetched joins
	 */
	private Map<String, SqmAttributeJoin> fetchesByPath;

	public FromClauseIndex() {
	}

	public void register(SqmPath<?> sqmPath, TableGroup tableGroup) {
		registerTableGroup( sqmPath.getNavigablePath(), tableGroup );

		if ( sqmPath.getExplicitAlias() != null ) {
			final TableGroup previousAliasReg = tableGroupByAliasXref.put( sqmPath.getExplicitAlias(), tableGroup );
			if ( previousAliasReg != null ) {
				log.debugf(
						"Encountered previous TableGroup registration [%s] for alias : %s",
						previousAliasReg,
						sqmPath.getExplicitAlias()
				);
			}
		}

		if ( sqmPath instanceof SqmAttributeJoin ) {
			final SqmAttributeJoin sqmJoin = (SqmAttributeJoin) sqmPath;
			if ( sqmJoin.isFetched() ) {
				registerJoinFetch( sqmJoin );
			}
		}
	}

	private void registerJoinFetch(SqmAttributeJoin sqmJoin) {
		if ( fetchesByPath == null ) {
			fetchesByPath = new HashMap<>();
		}
		NavigablePath navigablePath = sqmJoin.getNavigablePath();
		fetchesByPath.put( navigablePath.getIdentifierForTableGroup(), sqmJoin );
	}

	public boolean isResolved(SqmFrom fromElement) {
		return tableGroupMap.containsKey( fromElement.getNavigablePath() );
	}

	public SqmAttributeJoin findFetchedJoinByPath(NavigablePath path) {
		return fetchesByPath == null ? null : fetchesByPath.get( path.getIdentifierForTableGroup() );
	}
}
