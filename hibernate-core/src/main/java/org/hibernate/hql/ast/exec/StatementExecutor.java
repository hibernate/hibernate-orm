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
package org.hibernate.hql.ast.exec;

import org.hibernate.HibernateException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;

/**
 * Encapsulates the strategy required to execute various types of update, delete,
 * and insert statements issued through HQL.
 *
 * @author Steve Ebersole
 */
public interface StatementExecutor {

	public String[] getSqlStatements();

	/**
	 * Execute the sql managed by this executor using the given parameters.
	 *
	 * @param parameters Essentially bind information for this processing.
	 * @param session The session originating the request.
	 * @return The number of entities updated/deleted.
	 * @throws HibernateException
	 */
	public int execute(QueryParameters parameters, SessionImplementor session) throws HibernateException;

}
