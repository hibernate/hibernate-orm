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

import org.hibernate.QueryException;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;

/**
 * Defines a top-level AST node representing an HQL "insert select" statement.
 *
 * @author Steve Ebersole
 */
public class InsertStatement extends AbstractStatement {

	@Override
	public int getStatementType() {
		return HqlSqlTokenTypes.INSERT;
	}

	@Override
	public boolean needsExecutor() {
		return true;
	}

	/**
	 * Performs detailed semantic validation on this insert statement tree.
	 *
	 * @throws QueryException Indicates validation failure.
	 */
	public void validate() throws QueryException {
		getIntoClause().validateTypes( getSelectClause() );
	}

	/**
	 * Retrieve this insert statement's into-clause.
	 *
	 * @return The into-clause
	 */
	public IntoClause getIntoClause() {
		return (IntoClause) getFirstChild();
	}

	/**
	 * Retreive this insert statement's select-clause.
	 *
	 * @return The select-clause.
	 */
	public SelectClause getSelectClause() {
		return ( (QueryNode) getIntoClause().getNextSibling() ).getSelectClause();
	}

}
