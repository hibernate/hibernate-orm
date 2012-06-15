/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008 Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.sql.ordering.antlr;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for contextual information required to perform translation.
*
* @author Steve Ebersole
*/
public interface TranslationContext {
	/**
	 * Retrieves the <tt>session factory</tt> for this context.
	 *
	 * @return The <tt>session factory</tt>
	 */
	public SessionFactoryImplementor getSessionFactory();

	/**
	 * Retrieves the <tt>dialect</tt> for this context.
	 *
	 * @return The <tt>dialect</tt>
	 */
	public Dialect getDialect();

	/**
	 * Retrieves the <tt>SQL function registry/tt> for this context.
	 *
	 * @return The SQL function registry.
	 */
	public SQLFunctionRegistry getSqlFunctionRegistry();

	/**
	 * Retrieves the <tt>column mapper</tt> for this context.
	 *
	 * @return The <tt>column mapper</tt>
	 */
	public ColumnMapper getColumnMapper();
}
