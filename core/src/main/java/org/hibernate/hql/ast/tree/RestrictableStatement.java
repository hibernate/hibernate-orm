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
package org.hibernate.hql.ast.tree;

import antlr.collections.AST;

/**
 * Type definition for Statements which are restrictable via a where-clause (and
 * thus also having a from-clause).
 *
 * @author Steve Ebersole
 */
public interface RestrictableStatement extends Statement {
	/**
	 * Retreives the from-clause in effect for this statement.
	 *
	 * @return The from-clause for this statement; could be null if the from-clause
	 * has not yet been parsed/generated.
	 */
	public FromClause getFromClause();

	/**
	 * Does this statement tree currently contain a where clause?
	 *
	 * @return True if a where-clause is found in the statement tree and
	 * that where clause actually defines restrictions; false otherwise.
	 */
	public boolean hasWhereClause();

	/**
	 * Retreives the where-clause defining the restriction(s) in effect for
	 * this statement.
	 * <p/>
	 * Note that this will generate a where-clause if one was not found, so caution
	 * needs to taken prior to calling this that restrictions will actually exist
	 * in the resulting statement tree (otherwise "unexpected end of subtree" errors
	 * might occur during rendering).
	 *
	 * @return The where clause.
	 */
	public AST getWhereClause();
}
