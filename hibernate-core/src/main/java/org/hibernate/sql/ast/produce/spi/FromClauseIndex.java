/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.produce.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.spi.SimpleFromClauseAccessImpl;

import org.jboss.logging.Logger;

/**
 * An index of various FROM CLAUSE resolutions.
 *
 * @author Steve Ebersole
 */
public class FromClauseIndex extends SimpleFromClauseAccessImpl {
	private static final Logger log = Logger.getLogger( FromClauseIndex.class );

	public FromClauseIndex() {
	}

	private Map<NavigablePath,TableGroupJoin> tableGroupJoinMap;
	private final Map<String, TableGroup> tableGroupByAliasXref = new HashMap<>();

	/**
	 * Holds *explicitly* fetched joins
	 */
	private Map<NavigablePath, SqmAttributeJoin> fetchesByPath;
	private Map<NavigablePath, Map<NavigablePath, SqmAttributeJoin>> fetchesByParentPath;

	private final Set<String> affectedTableNames = new HashSet<>();

	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	public void register(SqmFrom sqmPath, TableGroup tableGroup) {
		if ( sqmPath instanceof SqmAttributeJoin ) {
			throw new IllegalArgumentException(
					"Passed SqmPath [" + sqmPath + "] is a SqmNavigableJoin - use the form of #register specific to joins"
			);
		}

		performRegistration( sqmPath, tableGroup );
	}

	private void performRegistration(SqmFrom sqmPath, TableGroup tableGroup) {
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
	}

	public boolean isResolved(SqmFrom fromElement) {
		return tableGroupMap.containsKey( fromElement.getNavigablePath() );
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		super.registerTableGroup( navigablePath, tableGroup );
		tableGroup.applyAffectedTableNames( affectedTableNames::add );
	}

	public void register(SqmAttributeJoin join, TableGroupJoin tableGroupJoin) {
		performRegistration( join, tableGroupJoin.getJoinedGroup() );

		if ( tableGroupJoinMap == null ) {
			tableGroupJoinMap = new HashMap<>();
		}
		tableGroupJoinMap.put( join.getNavigablePath(), tableGroupJoin );

		if ( join.isFetched() ) {
			if ( fetchesByPath == null ) {
				fetchesByPath = new HashMap<>();
			}
			fetchesByPath.put( join.getNavigablePath(), join );

			if ( fetchesByParentPath == null ) {
				fetchesByParentPath = new HashMap<>();
			}
			final Map<NavigablePath, SqmAttributeJoin> fetchesForParent = fetchesByParentPath.computeIfAbsent(
					join.getNavigablePath().getParent(),
					navigablePath -> new HashMap<>()
			);
			fetchesForParent.put( join.getNavigablePath(), join );
		}
	}

	public TableGroupJoin findTableGroupJoin(NavigablePath navigablePath) {
		return tableGroupJoinMap == null ? null : tableGroupJoinMap.get( navigablePath );
	}

	public SqmAttributeJoin findFetchedJoinByPath(NavigablePath path) {
		return fetchesByPath == null ? null : fetchesByPath.get( path );
	}
}
