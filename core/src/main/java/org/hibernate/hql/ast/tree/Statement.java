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

import org.hibernate.hql.ast.HqlSqlWalker;

/**
 * Common interface modeling the different HQL statements (i.e., INSERT, UPDATE, DELETE, SELECT).
 *
 * @author Steve Ebersole
 */
public interface Statement {

	/**
	 * Retreive the "phase 2" walker which generated this statement tree.
	 *
	 * @return The HqlSqlWalker instance which generated this statement tree.
	 */
	public HqlSqlWalker getWalker();

	/**
	 * Return the main token type representing the type of this statement.
	 *
	 * @return The corresponding token type.
	 */
	public int getStatementType();

	/**
	 * Does this statement require the StatementExecutor?
	 * </p>
	 * Essentially, at the JDBC level, does this require an executeUpdate()?
	 *
	 * @return True if this statement should be handed off to the
	 * StatementExecutor to be executed; false otherwise.
	 */
	public boolean needsExecutor();
}
