/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.hibernate.sql.sqm.ast.from.FromClause;
import org.hibernate.sql.sqm.ast.from.TableGroup;
import org.hibernate.sqm.path.AttributeBindingSource;
import org.hibernate.sqm.query.from.FromElement;

import org.jboss.logging.Logger;

/**
 * An index of various FROM CLAUSE resolutions.
 *
 * @author Steve Ebersole
 */
public class FromClauseIndex {
	private static final Logger log = Logger.getLogger( FromClauseIndex.class );

	private final Stack<FromClauseStackNode> fromClauseStackNodes = new Stack<>();

	private final Map<FromElement, TableGroup> fromElementTableSpecificationGroupXref = new HashMap<>();
	private final Map<AttributeBindingSource,TableGroup> attributeBindingSourceTableGroupXref = new HashMap<>();

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

	public void crossReference(FromElement fromElement, TableGroup tableGroup) {
		TableGroup old = fromElementTableSpecificationGroupXref.put( fromElement, tableGroup );
		if ( old != null ) {
			log.debugf(
					"FromElement [%s] was already cross-referenced to TableSpecificationGroup - old : [%s]; new : [%s]",
					fromElement,
					old,
					tableGroup
			);
		}
		crossReference( (AttributeBindingSource) fromElement, tableGroup );
	}

	public TableGroup findResolvedTableGroup(FromElement fromElement) {
		return fromElementTableSpecificationGroupXref.get( fromElement );
	}

	public void crossReference(AttributeBindingSource attributeBindingSource, TableGroup group) {
		attributeBindingSourceTableGroupXref.put( attributeBindingSource, group );
	}

	public TableGroup findResolvedTableGroup(AttributeBindingSource attributeBindingSource) {
		return attributeBindingSourceTableGroupXref.get( attributeBindingSource );
	}

	public boolean isResolved(FromElement fromElement) {
		return fromElementTableSpecificationGroupXref.containsKey( fromElement );
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
