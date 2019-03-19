/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.produce.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupResolver;
import org.hibernate.sql.ast.tree.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.spi.DomainResultCreationState;

import org.jboss.logging.Logger;

/**
 * An index of various FROM CLAUSE resolutions.
 *
 * @author Steve Ebersole
 */
public class FromClauseIndex
		extends DomainResultCreationState.SimpleFromClauseAccessImpl
		implements TableGroupResolver {

	// todo (6.0) : this could also act as the "affected table name" collecter
	//		- as we cross-ref TableGroups, add their table names

	public FromClauseIndex() {
	}

	private Map<NavigablePath,TableGroupJoin> tableGroupJoinMap;
	private final Map<String, TableGroup> tableGroupByAliasXref = new HashMap<>();

	/**
	 * Holds *explicitly* fetched joins
	 */
	private Map<NavigablePath,SqmNavigableJoin> fetchesByPath;
	private Map<NavigablePath, Map<NavigablePath, SqmNavigableJoin>> fetchesByParentPath;

	private final Set<String> affectedTableNames = new HashSet<>();

	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	private void newCrossReferencing(SqmFrom fromElement, TableGroup tableGroup) {
		register( fromElement, tableGroup );
	}

	public void register(SqmFrom sqmPath, TableGroup tableGroup) {
		if ( sqmPath instanceof SqmNavigableJoin ) {
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

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		super.registerTableGroup( navigablePath, tableGroup );
		tableGroup.applyAffectedTableNames( affectedTableNames::add );
	}

	public void register(SqmNavigableJoin join, TableGroupJoin tableGroupJoin) {
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
			final Map<NavigablePath, SqmNavigableJoin> fetchesForParent = fetchesByParentPath.computeIfAbsent(
					join.getNavigablePath().getParent(),
					navigablePath -> new HashMap<>()
			);
			fetchesForParent.put( join.getNavigablePath(), join );
		}
	}

	public TableGroupJoin findTableGroupJoin(NavigablePath navigablePath) {
		return tableGroupJoinMap == null ? null : tableGroupJoinMap.get( navigablePath );
	}

	public SqmNavigableJoin findFetchedJoinByPath(NavigablePath path) {
		return fetchesByPath == null ? null : fetchesByPath.get( path );
	}






	// todo (6.0) : Need to reconsider the cross referencing done here and decide how to index stuff:
	//		1) This grew out of SQM processing and currently its only consumer is still SQM processing.
	//			So perhaps we leave this SQM specific
	//		2) We do need a similar construct in "load plan" building, in which case we'd
	//			need a more generic cross-referencing most likely based on Navigable - especially for
	// 			fetches (see sqmFetchesByParentUid)


	// todo (6.0) : we should really leverage NavigablePath to key stuff here

	private static final Logger log = Logger.getLogger( FromClauseIndex.class );

	private final Map<NavigableReference,String> uidBySourceNavigableReference = new HashMap<>();
	private final Map<String,SqmFrom> sqmFromByUid = new HashMap<>();
	private final Map<SqmFrom, TableGroup> tableGroupBySqmFromXref = new HashMap<>();
	private Map<String, List<SqmNavigableJoin>> sqmFetchesByParentUid;


	@Override
	public TableGroup resolveTableGroup(String uid) {
		return findResolvedTableGroupByUniqueIdentifier( uid );
	}

	@Override
	public TableGroup resolveTableGroup(NavigablePath navigablePath) {
		return findTableGroup( navigablePath );
	}

	public void crossReference(SqmFrom fromElement, TableGroup tableGroup) {
		final SqmFrom existing = sqmFromByUid.put( fromElement.getUniqueIdentifier(), fromElement );
		if ( existing != null ) {
			if ( existing != fromElement ) {
				log.debugf(
						"Encountered duplicate SqmFrom#getUniqueIdentifier values [%s -> (%s, %s)]",
						fromElement.getUniqueIdentifier(),
						fromElement,
						existing
				);
			}
		}

		final TableGroup old = tableGroupBySqmFromXref.put( fromElement, tableGroup );
		if ( old != null ) {
			log.debugf(
					"FromElement [%s] was already cross-referenced to TableSpecificationGroup - old : [%s]; new : [%s]",
					fromElement,
					old,
					tableGroup
			);
		}

		if ( fromElement instanceof SqmNavigableJoin ) {
			final SqmNavigableJoin sqmAttributeJoin = (SqmNavigableJoin) fromElement;
			if ( sqmAttributeJoin.isFetched() ) {
				final String fetchParentUid = sqmAttributeJoin.getLhs().getUniqueIdentifier();

				if ( sqmFetchesByParentUid == null ) {
					sqmFetchesByParentUid = new HashMap<>();
				}

				List<SqmNavigableJoin> fetches = sqmFetchesByParentUid.computeIfAbsent(
						fetchParentUid,
						k -> new ArrayList<>()
				);
				fetches.add( sqmAttributeJoin );
			}
		}

		newCrossReferencing( fromElement, tableGroup );
	}

	public TableGroup findResolvedTableGroup(SqmFrom fromElement) {
		return resolveTableGroup( fromElement.getNavigablePath() );
	}

	public SqmFrom findSqmFromByUniqueIdentifier(String uniqueIdentifier) {
		return sqmFromByUid.get( uniqueIdentifier );
	}

	public TableGroup findResolvedTableGroupByUniqueIdentifier(String uniqueIdentifier) {
		final SqmFrom sqmFrom = findSqmFromByUniqueIdentifier( uniqueIdentifier );
		if ( sqmFrom == null ) {
			throw new ConversionException( "Could not resolve unique-identifier to SqmFrom" );
		}

		return tableGroupBySqmFromXref.get( sqmFrom );
	}


	public boolean isResolved(SqmFrom fromElement) {
		return tableGroupBySqmFromXref.containsKey( fromElement );
	}

	public List<SqmNavigableJoin> findFetchesByParentUniqueIdentifier(String uniqueIdentifier) {
		if ( sqmFetchesByParentUid == null ) {
			return Collections.emptyList();
		}

		final List<SqmNavigableJoin> fetches = sqmFetchesByParentUid.get( uniqueIdentifier );
		if ( fetches == null ) {
			return Collections.emptyList();
		}
		else {
			assert noDuplicates( fetches, uniqueIdentifier );
			return Collections.unmodifiableList( fetches );
		}
	}

	private boolean noDuplicates(List<SqmNavigableJoin> fetches, String uniqueIdentifier) {
		final Set<String> uniqueUids = fetches.stream()
				.map( SqmFrom::getUniqueIdentifier )
				.collect( Collectors.toSet() );
		if ( uniqueUids.size() != fetches.size() ) {
			throw new IllegalStateException( "Found duplicate fetches (by uid) for parent uid : " + uniqueIdentifier );
		}
		return true;
	}


	public NavigableReference findResolvedNavigableReference(NavigablePath navigablePath) {
		return null;
	}
}
