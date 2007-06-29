// $Id: QueryNode.java 7486 2005-07-15 04:39:41Z oneovthafew $
package org.hibernate.hql.ast.tree;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.util.ASTUtil;
import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a top-level AST node representing an HQL select statement.
 *
 * @author Joshua Davis
 */
public class QueryNode extends AbstractRestrictableStatement implements SelectExpression {

	private static final Log log = LogFactory.getLog( QueryNode.class );

	private OrderByClause orderByClause;

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

	protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.FROM;
	}

	protected Log getLog() {
		return log;
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
				log.debug( "getOrderByClause() : Creating a new ORDER BY clause" );
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

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	public Type getDataType() {
		return ( (SelectExpression) getSelectClause().getFirstSelectExpression() ).getDataType();
	}

}
