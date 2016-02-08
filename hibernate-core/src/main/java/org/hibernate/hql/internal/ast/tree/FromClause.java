/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTIterator;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents the 'FROM' part of a query or subquery, containing all mapped class references.
 *
 * @author josh
 */
public class FromClause extends HqlSqlWalkerNode implements HqlSqlTokenTypes, DisplayableNode {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( FromClause.class );

	public static final int ROOT_LEVEL = 1;

	private int level = ROOT_LEVEL;
	private Set<FromElement> fromElements = new HashSet<FromElement>();
	private Map<String,FromElement> fromElementByClassAlias = new HashMap<String,FromElement>();
	private Map<String,FromElement> fromElementByTableAlias = new HashMap<String,FromElement>();
	private Map<String,FromElement> fromElementsByPath = new HashMap<String,FromElement>();

	/**
	 * All of the implicit FROM xxx JOIN yyy elements that are the destination of a collection.  These are created from
	 * index operators on collection property references.
	 */
	private Map collectionJoinFromElementsByPath = new HashMap();
	/**
	 * Pointer to the parent FROM clause, if there is one.
	 */
	private FromClause parentFromClause;
	/**
	 * Collection of FROM clauses of which this is the parent.
	 */
	private Set<FromClause> childFromClauses;
	/**
	 * Counts the from elements as they are added.
	 */
	private int fromElementCounter;
	/**
	 * Implied FROM elements to add onto the end of the FROM clause.
	 */
	private List impliedElements = new LinkedList();

	private List<EntityJoinFromElement> entityJoinFromElements;

	/**
	 * Adds a new from element to the from node.
	 *
	 * @param path The reference to the class.
	 * @param alias The alias AST.
	 *
	 * @return FromElement - The new FROM element.
	 */
	public FromElement addFromElement(String path, AST alias) throws SemanticException {
		// The path may be a reference to an alias defined in the parent query.
		String classAlias = ( alias == null ) ? null : alias.getText();
		checkForDuplicateClassAlias( classAlias );
		FromElementFactory factory = new FromElementFactory( this, null, path, classAlias, null, false );
		return factory.addFromElement();
	}

	void registerFromElement(FromElement element) {
		fromElements.add( element );
		String classAlias = element.getClassAlias();
		if ( classAlias != null ) {
			// The HQL class alias refers to the class name.
			fromElementByClassAlias.put( classAlias, element );
		}
		// Associate the table alias with the element.
		String tableAlias = element.getTableAlias();
		if ( tableAlias != null ) {
			fromElementByTableAlias.put( tableAlias, element );
		}

		if ( element instanceof EntityJoinFromElement ) {
			if ( entityJoinFromElements == null ) {
				entityJoinFromElements = new ArrayList<EntityJoinFromElement>();
			}
			entityJoinFromElements.add( (EntityJoinFromElement) element );
		}
	}

	public void finishInit() {
		if ( entityJoinFromElements == null ) {
			return;
		}

		for ( EntityJoinFromElement entityJoinFromElement : entityJoinFromElements ) {
			ASTUtil.appendChild( this, entityJoinFromElement );
		}

		entityJoinFromElements.clear();
	}

	void addDuplicateAlias(String alias, FromElement element) {
		if ( alias != null ) {
			fromElementByClassAlias.put( alias, element );
		}
	}

	private void checkForDuplicateClassAlias(String classAlias) throws SemanticException {
		if ( classAlias != null && fromElementByClassAlias.containsKey( classAlias ) ) {
			throw new SemanticException( "Duplicate definition of alias '" + classAlias + "'" );
		}
	}

	/**
	 * Retrieve the from-element represented by the given alias.
	 *
	 * @param aliasOrClassName The alias by which to locate the from-element.
	 *
	 * @return The from-element assigned the given alias, or null if none.
	 */
	public FromElement getFromElement(String aliasOrClassName) {
		FromElement fromElement = fromElementByClassAlias.get( aliasOrClassName );
		if ( fromElement == null && getSessionFactoryHelper().isStrictJPAQLComplianceEnabled() ) {
			fromElement = findIntendedAliasedFromElementBasedOnCrazyJPARequirements( aliasOrClassName );
		}
		if ( fromElement == null && parentFromClause != null ) {
			fromElement = parentFromClause.getFromElement( aliasOrClassName );
		}
		return fromElement;
	}

	public FromElement findFromElementBySqlAlias(String sqlAlias) {
		FromElement fromElement = fromElementByTableAlias.get( sqlAlias );
		if ( fromElement == null && parentFromClause != null ) {
			fromElement = parentFromClause.getFromElement( sqlAlias );
		}
		return fromElement;
	}

	public FromElement findFromElementByUserOrSqlAlias(String userAlias, String sqlAlias) {
		FromElement fromElement = null;
		if ( userAlias != null ) {
			fromElement = getFromElement( userAlias );
		}

		if ( fromElement == null ) {
			fromElement = findFromElementBySqlAlias( sqlAlias );
		}

		return fromElement;
	}

	private FromElement findIntendedAliasedFromElementBasedOnCrazyJPARequirements(String specifiedAlias) {
		for ( Map.Entry<String, FromElement> entry : fromElementByClassAlias.entrySet() ) {
			final String alias = entry.getKey();
			if ( alias.equalsIgnoreCase( specifiedAlias ) ) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Convenience method to check whether a given token represents a from-element alias.
	 *
	 * @param possibleAlias The potential from-element alias to check.
	 *
	 * @return True if the possibleAlias is an alias to a from-element visible
	 *         from this point in the query graph.
	 */
	public boolean isFromElementAlias(String possibleAlias) {
		boolean isAlias = containsClassAlias( possibleAlias );
		if ( !isAlias && parentFromClause != null ) {
			// try the parent FromClause...
			isAlias = parentFromClause.isFromElementAlias( possibleAlias );
		}
		return isAlias;
	}

	/**
	 * Returns the list of from elements in order.
	 *
	 * @return the list of from elements (instances of FromElement).
	 */
	public List getFromElements() {
		return ASTUtil.collectChildren( this, fromElementPredicate );
	}

	public FromElement getFromElement() {
		// TODO: not sure about this one
//		List fromElements = getFromElements();
//		if ( fromElements == null || fromElements.isEmpty() ) {
//			throw new QueryException( "Unable to locate from element" );
//		}
		return (FromElement) getFromElements().get( 0 );
	}

	/**
	 * Returns the list of from elements that will be part of the result set.
	 *
	 * @return the list of from elements that will be part of the result set.
	 */
	public List getProjectionList() {
		return ASTUtil.collectChildren( this, projectionListPredicate );
	}

	public List getCollectionFetches() {
		return ASTUtil.collectChildren( this, collectionFetchPredicate );
	}

	public boolean hasCollectionFecthes() {
		return getCollectionFetches().size() > 0;
	}

	public List getExplicitFromElements() {
		return ASTUtil.collectChildren( this, explicitFromPredicate );
	}

	private static ASTUtil.FilterPredicate fromElementPredicate = new ASTUtil.IncludePredicate() {
		@Override
		public boolean include(AST node) {
			FromElement fromElement = (FromElement) node;
			return fromElement.isFromOrJoinFragment();
		}
	};

	private static ASTUtil.FilterPredicate projectionListPredicate = new ASTUtil.IncludePredicate() {
		@Override
		public boolean include(AST node) {
			FromElement fromElement = (FromElement) node;
			return fromElement.inProjectionList();
		}
	};

	private static ASTUtil.FilterPredicate collectionFetchPredicate = new ASTUtil.IncludePredicate() {
		@Override
		public boolean include(AST node) {
			FromElement fromElement = (FromElement) node;
			return fromElement.isFetch() && fromElement.getQueryableCollection() != null;
		}
	};

	private static ASTUtil.FilterPredicate explicitFromPredicate = new ASTUtil.IncludePredicate() {
		@Override
		public boolean include(AST node) {
			final FromElement fromElement = (FromElement) node;
			return !fromElement.isImplied();
		}
	};

	FromElement findCollectionJoin(String path) {
		return (FromElement) collectionJoinFromElementsByPath.get( path );
	}

	/**
	 * Look for an existing implicit or explicit join by the
	 * given path.
	 */
	FromElement findJoinByPath(String path) {
		FromElement elem = findJoinByPathLocal( path );
		if ( elem == null && parentFromClause != null ) {
			elem = parentFromClause.findJoinByPath( path );
		}
		return elem;
	}

	FromElement findJoinByPathLocal(String path) {
		Map joinsByPath = fromElementsByPath;
		return (FromElement) joinsByPath.get( path );
	}

	void addJoinByPathMap(String path, FromElement destination) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "addJoinByPathMap() : %s -> %s", path, destination.getDisplayText() );
		}
		fromElementsByPath.put( path, destination );
	}

	/**
	 * Returns true if the from node contains the class alias name.
	 *
	 * @param alias The HQL class alias name.
	 *
	 * @return true if the from node contains the class alias name.
	 */
	public boolean containsClassAlias(String alias) {
		boolean isAlias = fromElementByClassAlias.containsKey( alias );
		if ( !isAlias && getSessionFactoryHelper().isStrictJPAQLComplianceEnabled() ) {
			isAlias = findIntendedAliasedFromElementBasedOnCrazyJPARequirements( alias ) != null;
		}
		return isAlias;
	}

	/**
	 * Returns true if the from node contains the table alias name.
	 *
	 * @param alias The SQL table alias name.
	 *
	 * @return true if the from node contains the table alias name.
	 */
	public boolean containsTableAlias(String alias) {
		return fromElementByTableAlias.keySet().contains( alias );
	}

	public String getDisplayText() {
		return "FromClause{" +
				"level=" + level +
				", fromElementCounter=" + fromElementCounter +
				", fromElements=" + fromElements.size() +
				", fromElementByClassAlias=" + fromElementByClassAlias.keySet() +
				", fromElementByTableAlias=" + fromElementByTableAlias.keySet() +
				", fromElementsByPath=" + fromElementsByPath.keySet() +
				", collectionJoinFromElementsByPath=" + collectionJoinFromElementsByPath.keySet() +
				", impliedElements=" + impliedElements +
				"}";
	}

	public void setParentFromClause(FromClause parentFromClause) {
		this.parentFromClause = parentFromClause;
		if ( parentFromClause != null ) {
			level = parentFromClause.getLevel() + 1;
			parentFromClause.addChild( this );
		}
	}

	private void addChild(FromClause fromClause) {
		if ( childFromClauses == null ) {
			childFromClauses = new HashSet<FromClause>();
		}
		childFromClauses.add( fromClause );
	}

	public FromClause locateChildFromClauseWithJoinByPath(String path) {
		if ( childFromClauses != null && !childFromClauses.isEmpty() ) {
			for ( FromClause child : childFromClauses ) {
				if ( child.findJoinByPathLocal( path ) != null ) {
					return child;
				}
			}
		}
		return null;
	}

	public void promoteJoin(FromElement elem) {
		LOG.debugf( "Promoting [%s] to [%s]", elem, this );
		//TODO: implement functionality
		//  this might be painful to do here, as the "join post processing" for
		//  the subquery has already been performed (meaning that for
		//  theta-join dialects, the join conditions have already been moved
		//  over to the where clause).  A "simple" solution here might to
		//  doAfterTransactionCompletion "join post processing" once for the entire query (including
		//  any subqueries) at one fell swoop
	}

	public boolean isSubQuery() {
		// TODO : this is broke for subqueries in statements other than selects...
		return parentFromClause != null;
	}

	void addCollectionJoinFromElementByPath(String path, FromElement destination) {
		LOG.debugf( "addCollectionJoinFromElementByPath() : %s -> %s", path, destination );
		collectionJoinFromElementsByPath.put(
				path,
				destination
		);    // Add the new node to the map so that we don't create it twice.
	}

	public FromClause getParentFromClause() {
		return parentFromClause;
	}

	public int getLevel() {
		return level;
	}

	public int nextFromElementCounter() {
		return fromElementCounter++;
	}

	public void resolve() {
		// Make sure that all from elements registered with this FROM clause are actually in the AST.
		ASTIterator iter = new ASTIterator( this.getFirstChild() );
		Set childrenInTree = new HashSet();
		while ( iter.hasNext() ) {
			childrenInTree.add( iter.next() );
		}
		for ( FromElement fromElement : fromElements ) {
			if ( !childrenInTree.contains( fromElement ) ) {
				throw new IllegalStateException( "Element not in AST: " + fromElement );
			}
		}
	}

	public void addImpliedFromElement(FromElement element) {
		impliedElements.add( element );
	}

	@Override
	public String toString() {
		return "FromClause{level=" + level + "}";
	}
}
