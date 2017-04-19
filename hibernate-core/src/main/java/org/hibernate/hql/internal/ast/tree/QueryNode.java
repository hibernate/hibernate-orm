/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Defines a top-level AST node representing an HQL select statement.
 *
 * @author Joshua Davis
 */
public class QueryNode extends AbstractRestrictableStatement implements SelectExpression {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QueryNode.class );

	private OrderByClause orderByClause;
	private int scalarColumnIndex = -1;

	@Override
	public int getStatementType() {
		return HqlSqlTokenTypes.QUERY;
	}

	@Override
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
		return (SelectClause) ASTUtil.findTypeInChildren( this, SqlTokenTypes.SELECT_CLAUSE );
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
				orderByClause = (OrderByClause) getWalker().getASTFactory().create( SqlTokenTypes.ORDER, "ORDER" );

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
		return (OrderByClause) ASTUtil.findTypeInChildren( this, SqlTokenTypes.ORDER );
	}


	private String alias;

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public FromElement getFromElement() {
		return null;
	}

	@Override
	public boolean isConstructor() {
		return false;
	}

	@Override
	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	@Override
	public boolean isScalar() throws SemanticException {
		return true;
	}

	@Override
	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public void setScalarColumn(int i) throws SemanticException {
		scalarColumnIndex = i;
		setScalarColumnText( i );
	}

	@Override
	public int getScalarColumnIndex() {
		return scalarColumnIndex;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	@Override
	public Type getDataType() {
		return ( (SelectExpression) getSelectClause().getFirstSelectExpression() ).getDataType();
	}

}
