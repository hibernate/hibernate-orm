/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;

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

	/**
	 *  {@inheritDoc}
	 */
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
		return StringHelper.join( ", ", getWalker().getSelectClause().getColumnNames()[scalarColumnIndex] );
	}
}
