/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.hql.internal.ast.tree;
import antlr.SemanticException;
import antlr.collections.AST;
import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;

/**
 * Represents a reference to a FROM element, for example a class alias in a WHERE clause.
 *
 * @author josh
 */
public abstract class FromReferenceNode extends AbstractSelectExpression
        implements ResolvableNode, DisplayableNode, InitializeableNode, PathNode {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, FromReferenceNode.class.getName() );

	private FromElement fromElement;
	private boolean resolved = false;
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

	public String getDisplayText() {
		StringBuilder buf = new StringBuilder();
		buf.append( "{" ).append( ( fromElement == null ) ? "no fromElement" : fromElement.getDisplayText() );
		buf.append( "}" );
		return buf.toString();
	}

	public void recursiveResolve(int level, boolean impliedAtRoot, String classAlias) throws SemanticException {
		recursiveResolve( level, impliedAtRoot, classAlias, this );
	}

	public void recursiveResolve(int level, boolean impliedAtRoot, String classAlias, AST parent) throws SemanticException {
		AST lhs = getFirstChild();
		int nextLevel = level + 1;
		if ( lhs != null ) {
			FromReferenceNode n = ( FromReferenceNode ) lhs;
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

	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		resolve( generateJoin, implicitJoin );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		resolve( generateJoin, implicitJoin, null );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias) throws SemanticException {
		resolve( generateJoin, implicitJoin, classAlias, null );
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

}
