/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import antlr.SemanticException;

/**
 * Represents a reference to a result_variable as defined in the JPA 2 spec.
 * For example:
 * <code>
 * select v as value from tab1 order by value
 * </code>
 * <p/>
 * "value" used in the order by clause is a reference to the
 * result_variable, "value", defined in the select clause.
 *
 * @author Gail Badner
 */
public class ResultVariableRefNode extends HqlSqlWalkerNode {
	private SelectExpression selectExpression;

	/**
	 * Set the select expression that defines the result variable.
	 *
	 * @param selectExpression the select expression;
	 *        selectExpression.getAlias() must be non-null
	 * @throws SemanticException if selectExpression or
	 *         selectExpression.getAlias() is null.
	 */
	public void setSelectExpression(SelectExpression selectExpression) throws SemanticException {
		if ( selectExpression == null || selectExpression.getAlias() == null ) {
			throw new SemanticException( "A ResultVariableRefNode must refer to a non-null alias." );
		}
		this.selectExpression = selectExpression;
	}

	@Override
	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		int scalarColumnIndex = selectExpression.getScalarColumnIndex();
		if ( scalarColumnIndex < 0 ) {
			throw new IllegalStateException(
					"selectExpression.getScalarColumnIndex() must be >= 0; actual = " + scalarColumnIndex
			);
		}
		return sessionFactory.getDialect().replaceResultVariableInOrderByClauseWithPosition() ?
			getColumnPositionsString( scalarColumnIndex ) :
			getColumnNamesString( scalarColumnIndex );

	}

	private String getColumnPositionsString(int scalarColumnIndex ) {
		int startPosition = getWalker().getSelectClause().getColumnNamesStartPosition( scalarColumnIndex );
		StringBuilder buf = new StringBuilder();
		int nColumns = getWalker().getSelectClause().getColumnNames()[ scalarColumnIndex ].length;
		for ( int i = startPosition; i < startPosition + nColumns; i++ ) {
			if ( i > startPosition ) {
				buf.append( ", " );
			}
			buf.append( i );
		}
		return buf.toString();
	}

	private String getColumnNamesString(int scalarColumnIndex) {
		return String.join( ", ", getWalker().getSelectClause().getColumnNames()[scalarColumnIndex] );
	}
}
