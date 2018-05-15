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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

import org.jboss.logging.Logger;

/**
 * An index of various FROM CLAUSE resolutions.
 *
 * todo (6.0) : but the problem is that this only works for SQM building of a SQL AST but is passed around to the generic
 *
 * @author Steve Ebersole
 */
public class FromClauseIndex implements TableGroupResolver {


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

	public String getTableGroupUidForNavigableReference(NavigableReference navigableReference) {
		return uidBySourceNavigableReference.get( navigableReference );
	}

	public void crossReference(
			SqmFrom fromElement,
			TableGroup tableGroup) {
		SqmFrom existing = sqmFromByUid.put( fromElement.getUniqueIdentifier(), fromElement );
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

		uidBySourceNavigableReference.put( tableGroup.getNavigableReference(), tableGroup.getUniqueIdentifier() );

		TableGroup old = tableGroupBySqmFromXref.put( fromElement, tableGroup );
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
				final String fetchParentUid = sqmAttributeJoin.getNavigableReference().getNavigableContainerReferenceInfo().getUniqueIdentifier();

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
	}

	public TableGroup findResolvedTableGroup(SqmFrom fromElement) {
		return fromElement.locateMapping( this );

//		// todo (6.0) : this is a hacky solution/workaround to the fact that SQM creates joins for composites/embeddables whereas we do not in the SQL-AST
//		//		so the cross referencing is off
//
//		TableGroup tableGroup = tableGroupBySqmFromXref.get( fromElement );
//		while ( tableGroup == null
//				&& fromElement != null
//				&& fromElement.getNavigableReference().getReferencedNavigable() instanceof EmbeddedValuedNavigable ) {
//			fromElement = fromElement.getNavigableReference().getSourceReference().getExportedFromElement();
//			tableGroup = tableGroupBySqmFromXref.get( fromElement );
//		}
//		return tableGroup;
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
