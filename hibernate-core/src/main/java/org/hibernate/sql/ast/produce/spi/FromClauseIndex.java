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
import java.util.Stack;
import java.util.stream.Collectors;

import org.hibernate.persister.queryable.spi.NavigableReferenceInfo;
import org.hibernate.persister.queryable.spi.TableGroupResolver;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.from.FromClause;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * An index of various FROM CLAUSE resolutions.
 *
 * todo (6.0) : but the problem is that this only works for SQM building of a SQL AST but is passed around to the generic
 *
 * @author Steve Ebersole
 */
public class FromClauseIndex implements TableGroupResolver {
	private static final Logger log = Logger.getLogger( FromClauseIndex.class );

	private final Stack<FromClauseStackNode> fromClauseStackNodes = new Stack<>();

	private final Map<String,NavigableReferenceInfo> navigableRefInfoByUid = new HashMap<>();
	private final Map<String,ColumnReferenceSource> columnRefSourceByUid = new HashMap<>();

	private final Map<NavigableReferenceInfo,ColumnReferenceSource> colRefSourceByNavRefInfo = new HashMap<>();


	// todo (6.0) : Need to reconsider the cross referencing done here and decide how to index stuff:
	//		1) This grew out of SQM processing and currently its only consumer is still SQM processing.
	//			So perhaps we leave this SQM specific
	//		2) We do need a similar construct in "load plan" building, in which case we'd
	//			need a more generic cross-referencing most likely based on Navigable - especially for
	// 			fetches (see sqmFetchesByParentUid)


	@Override
	public ColumnReferenceSource resolveColumnReferenceSource(String uid) {
		return columnRefSourceByUid.get( uid );
	}

	@Override
	public NavigableReferenceInfo resolveNavigableReferenceInfo(String uid) {
		return navigableRefInfoByUid.get( uid );
	}

















	private final Map<String,SqmFrom> sqmFromByUid = new HashMap<>();
	private final Map<SqmFrom, TableGroup> tableGroupBySqmFromXref = new HashMap<>();
	private Map<String, List<SqmAttributeJoin>> sqmFetchesByParentUid;


	public void pushFromClause(FromClause fromClause) {
		FromClauseStackNode parent = null;
		if ( fromClauseStackNodes.size() > 0 ) {
			parent = fromClauseStackNodes.peek();
		}
		FromClauseStackNode node = new FromClauseStackNode( parent, fromClause );
		fromClauseStackNodes.push( node );
	}

	public FromClause popFromClause() {
		final FromClauseStackNode node = fromClauseStackNodes.pop();
		if ( node == null ) {
			return null;
		}
		else {
			return node.getCurrentFromClause();
		}
	}

	public FromClause currentFromClause() {
		final FromClauseStackNode currentNode = fromClauseStackNodes.peek();
		if ( currentNode == null ) {
			return null;
		}
		else {
			return currentNode.getCurrentFromClause();
		}
	}

	@Override
	public TableGroup resolveTableGroup(String uid) {
		return findResolvedTableGroupByUniqueIdentifier( uid );
	}

	public void crossReference(SqmFrom fromElement, TableGroup tableGroup) {
		TableGroup old = tableGroupBySqmFromXref.put( fromElement, tableGroup );
		if ( old != null ) {
			log.debugf(
					"FromElement [%s] was already cross-referenced to TableSpecificationGroup - old : [%s]; new : [%s]",
					fromElement,
					old,
					tableGroup
			);
		}

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

		if ( fromElement instanceof SqmAttributeJoin ) {
			final SqmAttributeJoin sqmAttributeJoin = (SqmAttributeJoin) fromElement;
			if ( sqmAttributeJoin.isFetched() ) {
				final String fetchParentUid = sqmAttributeJoin.getNavigableContainerReferenceInfo().getUniqueIdentifier();

				if ( sqmFetchesByParentUid == null ) {
					sqmFetchesByParentUid = new HashMap<>();
				}

				List<SqmAttributeJoin> fetches = sqmFetchesByParentUid.computeIfAbsent(
						fetchParentUid,
						k -> new ArrayList<>()
				);
				fetches.add( sqmAttributeJoin );
			}
		}

		crossReference( fromElement.getNavigableReference(), tableGroup );
	}

	public TableGroup findResolvedTableGroup(SqmFrom fromElement) {
		return tableGroupBySqmFromXref.get( fromElement );
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

	public void crossReference(SqmNavigableReference binding, TableGroup group) {
		tableGroupBySqmFromXref.put( binding.getFromElement(), group );
	}

	public TableGroup findResolvedTableGroup(SqmNavigableReference binding) {
		if ( binding == null ) {
			// todo : or error?
			return null;
		}

		TableGroup tableGroup = findResolvedTableGroup( binding.getFromElement() );
		if ( tableGroup == null ) {
			if ( binding instanceof SqmSingularAttributeReference ) {
				// it might be a composite...
				final SqmSingularAttributeReference singularAttributeBinding = (SqmSingularAttributeReference) binding;
				final SqmSingularAttributeReference.SingularAttributeClassification classification = singularAttributeBinding
						.getAttribute()
						.getAttributeTypeClassification();
				if ( classification == SingularAttributeReference.SingularAttributeClassification.EMBEDDED ) {
					tableGroup = findResolvedTableGroup( singularAttributeBinding.getLhs() );
				}
			}
		}

		return tableGroup;
	}

	public boolean isResolved(SqmFrom fromElement) {
		return tableGroupBySqmFromXref.containsKey( fromElement );
	}

	public List<SqmAttributeJoin> findFetchesByUniqueIdentifier(String uniqueIdentifier) {
		if ( sqmFetchesByParentUid == null ) {
			return Collections.emptyList();
		}

		final List<SqmAttributeJoin> fetches = sqmFetchesByParentUid.get( uniqueIdentifier );
		if ( fetches == null ) {
			return Collections.emptyList();
		}
		else {
			assert noDuplicates( fetches, uniqueIdentifier );
			return Collections.unmodifiableList( fetches );
		}
	}

	private boolean noDuplicates(List<SqmAttributeJoin> fetches, String uniqueIdentifier) {
		final Set<String> uniqueUids = fetches.stream()
				.map( AbstractFrom::getUniqueIdentifier )
				.collect( Collectors.toSet() );
		if ( uniqueUids.size() != fetches.size() ) {
			throw new IllegalStateException( "Found duplicate fetches (by uid) for parent uid : " + uniqueIdentifier );
		}
		return true;
	}

	public static class FromClauseStackNode {
		private final FromClauseStackNode parentNode;
		private final FromClause currentFromClause;

		public FromClauseStackNode(FromClause currentFromClause) {
			this( null, currentFromClause );
		}

		public FromClauseStackNode(FromClauseStackNode parentNode, FromClause currentFromClause) {
			this.parentNode = parentNode;
			this.currentFromClause = currentFromClause;
		}

		public FromClauseStackNode getParentNode() {
			return parentNode;
		}

		public FromClause getCurrentFromClause() {
			return currentFromClause;
		}
	}

}
