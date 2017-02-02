/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
