/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.hql.internal.ast.InvalidPathException;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents a `fk()` pseudo-function
 *
 * @author Steve Ebersole
 */
public class FkRefNode
		extends HqlSqlWalkerNode
		implements ResolvableNode, DisplayableNode, PathNode {
	private FromReferenceNode toOnePath;

	private Type fkType;
	private String[] columns;

	private FromReferenceNode resolveToOnePath() {
		if ( toOnePath == null ) {
			try {
				resolve( false, true );
			}
			catch (SemanticException e) {
				final String msg = "Unable to resolve to-one path `fk(" + toOnePath.getPath() + "`)";
				throw new QueryException( msg, new InvalidPathException( msg ) );
			}
		}

		assert toOnePath != null;
		return toOnePath;
	}

	@Override
	public String getDisplayText() {
		final FromReferenceNode toOnePath = resolveToOnePath();
		return "fk(`" + toOnePath.getDisplayText() + "` )";
	}

	@Override
	public String getPath() {
		return toOnePath.getDisplayText() + ".{fk}";
	}

	@Override
	public void resolve(
			boolean generateJoin,
			boolean implicitJoin) throws SemanticException {
		if ( toOnePath != null ) {
			return;
		}

		final AST firstChild = getFirstChild();
		assert firstChild instanceof FromReferenceNode;

		toOnePath = (FromReferenceNode) firstChild;
		toOnePath.resolve( false, true, null, toOnePath.getFromElement() );

		final Type sourcePathDataType = toOnePath.getDataType();
		if ( ! ( sourcePathDataType instanceof ManyToOneType ) ) {
			throw new InvalidPathException(
					"Argument to fk() function must be a to-one path, but found " + sourcePathDataType
			);
		}
		final ManyToOneType toOneType = (ManyToOneType) sourcePathDataType;
		final FromElement fromElement = toOnePath.getFromElement();

		fkType = toOneType.getIdentifierOrUniqueKeyType( getSessionFactoryHelper().getFactory() );
		assert fkType instanceof BasicType
				|| fkType instanceof CompositeType;

		columns = fromElement.getElementType().toColumns(
				fromElement.getTableAlias(),
				toOneType.getPropertyName(),
				getWalker().isInSelect()
		);
		assert columns != null && columns.length > 0;

		setText( String.join( ", ", columns ) );
	}

	@Override
	public void resolve(
			boolean generateJoin,
			boolean implicitJoin,
			String classAlias,
			AST parent,
			AST parentPredicate) throws SemanticException {
		resolve( false, true );
	}

	@Override
	public void resolve(
			boolean generateJoin,
			boolean implicitJoin,
			String classAlias,
			AST parent) throws SemanticException {
		resolve( false, true );
	}

	@Override
	public void resolve(
			boolean generateJoin,
			boolean implicitJoin,
			String classAlias) throws SemanticException {
		resolve( false, true );
	}

	@Override
	public void resolveInFunctionCall(
			boolean generateJoin,
			boolean implicitJoin) throws SemanticException {
		resolve( false, true );
	}

	@Override
	public void resolveIndex(AST parent) throws SemanticException {
		throw new InvalidPathException( "fk() paths cannot be de-referenced as indexed path" );
	}
}
