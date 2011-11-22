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

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;

/**
 * Defines a top-level AST node representing an HQL select statement.
 *
 * @author Joshua Davis
 */
public class QueryNode extends AbstractRestrictableStatement implements SelectExpression {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, QueryNode.class.getName());

	private OrderByClause orderByClause;
	private int scalarColumnIndex = -1;

	/**
	 * @see Statement#getStatementType()
	 */
	public int getStatementType() {
		return HqlSqlTokenTypes.QUERY;
	}

	/**
	 * @see Statement#needsExecutor()
	 */
	public boolean needsExecutor() {
		return false;
	}

	@Override
    protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.FROM;
	}

	@Override
    protected CoreMessageLogger getLog() {
        return LOG;
	}

	/**
	 * Locate the select clause that is part of this select statement.
	 * </p>
	 * Note, that this might return null as derived select clauses (i.e., no
	 * select clause at the HQL-level) get generated much later than when we
	 * get created; thus it depends upon lifecycle.
	 *
	 * @return Our select clause, or null.
	 */
	public final SelectClause getSelectClause() {
		// Due to the complexity in initializing the SelectClause, do not generate one here.
		// If it is not found; simply return null...
		//
		// Also, do not cache since it gets generated well after we are created.
		return ( SelectClause ) ASTUtil.findTypeInChildren( this, SqlTokenTypes.SELECT_CLAUSE );
	}

	public final boolean hasOrderByClause() {
		OrderByClause orderByClause = locateOrderByClause();
		return orderByClause != null && orderByClause.getNumberOfChildren() > 0;
	}

	public final OrderByClause getOrderByClause() {
		if ( orderByClause == null ) {
			orderByClause = locateOrderByClause();

			// if there is no order by, make one
			if ( orderByClause == null ) {
				LOG.debug( "getOrderByClause() : Creating a new ORDER BY clause" );
				orderByClause = ( OrderByClause ) ASTUtil.create( getWalker().getASTFactory(), SqlTokenTypes.ORDER, "ORDER" );

				// Find the WHERE; if there is no WHERE, find the FROM...
				AST prevSibling = ASTUtil.findTypeInChildren( this, SqlTokenTypes.WHERE );
				if ( prevSibling == null ) {
					prevSibling = ASTUtil.findTypeInChildren( this, SqlTokenTypes.FROM );
				}

				// Now, inject the newly built ORDER BY into the tree
				orderByClause.setNextSibling( prevSibling.getNextSibling() );
				prevSibling.setNextSibling( orderByClause );
			}
		}
		return orderByClause;
	}

	private OrderByClause locateOrderByClause() {
		return ( OrderByClause ) ASTUtil.findTypeInChildren( this, SqlTokenTypes.ORDER );
	}


	private String alias;

	public String getAlias() {
		return alias;
	}

	public FromElement getFromElement() {
		return null;
	}

	public boolean isConstructor() {
		return false;
	}

	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	public boolean isScalar() throws SemanticException {
		return true;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public void setScalarColumn(int i) throws SemanticException {
		scalarColumnIndex = i;
		setScalarColumnText( i );
	}

	public int getScalarColumnIndex() {
		return scalarColumnIndex;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	@Override
    public Type getDataType() {
		return ( (SelectExpression) getSelectClause().getFirstSelectExpression() ).getDataType();
	}

}
