/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Represents a cast function call.  We handle this specially because its type
 * argument has a semantic meaning to the HQL query (its not just pass through).
 *
 * @author Steve Ebersole
 */
public class CastFunctionNode extends AbstractSelectExpression implements FunctionNode {
	private SQLFunction dialectCastFunction;

	private Node expressionNode;

	private IdentNode typeNode;
	private Type castType;


	/**
	 * Called from the hql-sql grammar after the children of the CAST have been resolved.
	 *
	 * @param inSelect Is this call part of the SELECT clause?
	 */
	public void resolve(boolean inSelect) {
		this.dialectCastFunction = getSessionFactoryHelper().findSQLFunction( "cast" );
		if ( dialectCastFunction == null ) {
			dialectCastFunction = CastFunction.INSTANCE;
		}

		this.expressionNode = (Node) getFirstChild();
		if ( expressionNode == null ) {
			throw new QueryException( "Could not resolve expression to CAST" );
		}
		if ( SqlNode.class.isInstance( expressionNode ) ) {
			final Type expressionType = ( (SqlNode) expressionNode ).getDataType();
			if ( expressionType != null ) {
				if ( expressionType.isEntityType() ) {
					throw new QueryException( "Expression to CAST cannot be an entity : " + expressionNode.getText() );
				}
				if ( expressionType.isComponentType() ) {
					throw new QueryException( "Expression to CAST cannot be a composite : " + expressionNode.getText() );
				}
				if ( expressionType.isCollectionType() ) {
					throw new QueryException( "Expression to CAST cannot be a collection : " + expressionNode.getText() );
				}
			}
		}

		this.typeNode = (IdentNode) expressionNode.getNextSibling();
		if ( typeNode == null ) {
			throw new QueryException( "Could not resolve requested type for CAST" );
		}

		final String typeName = typeNode.getText();
		this.castType = getSessionFactoryHelper().getFactory().getTypeResolver().heuristicType( typeName );
		if ( castType == null ) {
			throw new QueryException( "Could not resolve requested type for CAST : " + typeName );
		}
		if ( castType.isEntityType() ) {
			throw new QueryException( "CAST target type cannot be an entity : " + expressionNode.getText() );
		}
		if ( castType.isComponentType() ) {
			throw new QueryException( "CAST target type cannot be a composite : " + expressionNode.getText() );
		}
		if ( castType.isCollectionType() ) {
			throw new QueryException( "CAST target type cannot be a collection : " + expressionNode.getText() );
		}
		setDataType( castType );
	}

	@Override
	public SQLFunction getSQLFunction() {
		return dialectCastFunction;
	}

	@Override
	public Type getFirstArgumentType() {
		return castType;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}
}
