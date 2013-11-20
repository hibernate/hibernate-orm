/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Contract for processing JDBC ResultSets a single logical row at a time.  These are intended for use by
 * {@link org.hibernate.ScrollableResults} implementations.
 *
 * NOTE : these methods initially taken directly from {@link org.hibernate.loader.Loader} counterparts in an effort
 * to break Loader into manageable pieces, especially in regards to the processing of result sets.
 *
 * @author Steve Ebersole
 */
public interface ScrollableResultSetProcessor {

	/**
	 * Give a ResultSet, extract just a single result row.
	 *
	 * Copy of {@link org.hibernate.loader.Loader#loadSingleRow(java.sql.ResultSet, org.hibernate.engine.spi.SessionImplementor, org.hibernate.engine.spi.QueryParameters, boolean)}
	 * but dropping the 'returnProxies' (that method has only one use in the entire codebase and it always passes in
	 * false...)
	 *
	 * @param resultSet The result set being processed.
	 * @param session The originating session
	 * @param queryParameters The "parameters" used to build the query
	 *
	 * @return The extracted result row
	 *
	 * @throws org.hibernate.HibernateException Indicates a problem extracting values from the result set.
	 */
	public Object extractSingleRow(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters);

	/**
	 * Given a scrollable ResultSet, extract a logical row.  The assumption here is that the ResultSet is already
	 * properly ordered to account for any to-many fetches.  Multiple ResultSet rows are read into a single query
	 * result "row".
	 *
	 * Copy of {@link org.hibernate.loader.Loader#loadSequentialRowsForward(java.sql.ResultSet, org.hibernate.engine.spi.SessionImplementor, org.hibernate.engine.spi.QueryParameters, boolean)}
	 * but dropping the 'returnProxies' (that method has only one use in the entire codebase and it always passes in
	 * false...)
	 *
	 * @param resultSet The result set being processed.
	 * @param session The originating session
	 * @param queryParameters The "parameters" used to build the query
	 *
	 * @return The extracted result row
	 *
	 * @throws org.hibernate.HibernateException Indicates a problem extracting values from the result set.
	 */
	public Object extractLogicalRowForward(
			final ResultSet resultSet,
			final SessionImplementor session,
			final QueryParameters queryParameters);

	/**
	 * Like {@link #extractLogicalRowForward} but here moving through the ResultSet in reverse.
	 *
	 * Copy of {@link org.hibernate.loader.Loader#loadSequentialRowsReverse(java.sql.ResultSet, org.hibernate.engine.spi.SessionImplementor, org.hibernate.engine.spi.QueryParameters, boolean, boolean)}
	 * but dropping the 'returnProxies' (that method has only one use in the entire codebase and it always passes in
	 * false...).
	 *
	 * todo : is 'logicallyAfterLastRow really needed?  Can't that be deduced?  In fact pretty positive it is not needed.
	 *
	 * @param resultSet The result set being processed.
	 * @param session The originating session
	 * @param queryParameters The "parameters" used to build the query
	 * @param isLogicallyAfterLast Is the result set currently positioned after the last row; again, is this really needed?  How is it any diff
	 *
	 * @return The extracted result row
	 *
	 * @throws org.hibernate.HibernateException Indicates a problem extracting values from the result set.
	 */
	public Object extractLogicalRowReverse(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters,
			boolean isLogicallyAfterLast);
}
