/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents a reference to a FROM element, for example a class alias in a WHERE clause.
 *
 * @author josh
 */
public abstract class FromReferenceNode extends AbstractSelectExpression
		implements ResolvableNode, DisplayableNode, InitializeableNode, PathNode {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( FromReferenceNode.class );

	private FromElement fromElement;
	private boolean resolved;

	public static final int ROOT_LEVEL = 0;

	@Override
	public FromElement getFromElement() {
		return fromElement;
	}

	public void setFromElement(FromElement fromElement) {
		this.fromElement = fromElement;
	}

	/**
	 * Resolves the left hand side of the DOT.
	 *
	 * @throws SemanticException
	 */
	public void resolveFirstChild() throws SemanticException {
	}

	@Override
	public String getPath() {
		return getOriginalText();
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved() {
		this.resolved = true;
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Resolved : %s -> %s", this.getPath(), this.getText() );
		}
	}

	@Override
	public String getDisplayText() {
		StringBuilder buf = new StringBuilder();
		buf.append( "{" ).append( ( fromElement == null ) ? "no fromElement" : fromElement.getDisplayText() );
		buf.append( "}" );
		return buf.toString();
	}

	public void recursiveResolve(int level, boolean impliedAtRoot, String classAlias) throws SemanticException {
		recursiveResolve( level, impliedAtRoot, classAlias, this );
	}

	public void recursiveResolve(int level, boolean impliedAtRoot, String classAlias, AST parent)
			throws SemanticException {
		AST lhs = getFirstChild();
		int nextLevel = level + 1;
		if ( lhs != null ) {
			FromReferenceNode n = (FromReferenceNode) lhs;
			n.recursiveResolve( nextLevel, impliedAtRoot, null, this );
		}
		resolveFirstChild();
		boolean impliedJoin = true;
		if ( level == ROOT_LEVEL && !impliedAtRoot ) {
			impliedJoin = false;
		}
		resolve( true, impliedJoin, classAlias, parent );
	}

	@Override
	public boolean isReturnableEntity() throws SemanticException {
		return !isScalar() && fromElement.isEntity();
	}

	@Override
	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		resolve( generateJoin, implicitJoin );
	}

	@Override
	public void resolve(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		resolve( generateJoin, implicitJoin, null );
	}

	@Override
	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias) throws SemanticException {
		resolve( generateJoin, implicitJoin, classAlias, null );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent)
			throws SemanticException {
		resolve( generateJoin, implicitJoin, classAlias, parent, null );
	}

	public void prepareForDot(String propertyName) throws SemanticException {
	}

	/**
	 * Sub-classes can override this method if they produce implied joins (e.g. DotNode).
	 *
	 * @return an implied join created by this from reference.
	 */
	public FromElement getImpliedJoin() {
		return null;
	}

	@SuppressWarnings("SimplifiableIfStatement")
	protected boolean isFromElementUpdateOrDeleteRoot(FromElement element) {
		if ( element.getFromClause().getParentFromClause() != null ) {
			// its not even a root...
			return false;
		}

		return getWalker().getStatementType() == HqlSqlTokenTypes.DELETE
				|| getWalker().getStatementType() == HqlSqlTokenTypes.UPDATE;
	}

}
