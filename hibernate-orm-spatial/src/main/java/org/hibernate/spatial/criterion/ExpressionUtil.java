/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;

/**
 * This class assists in the formation of a SQL-fragment in the various spatial query expressions.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 2/15/11
 */
public class ExpressionUtil {

	/**
	 * private constructor prevents instantiation of this utility class
	 */
	private ExpressionUtil() {
	}

	/**
	 * Determines the {@code SpatialDialect} for the specified {@code CriteriaQuery}, and checks if the
	 * specified function is supported.
	 *
	 * @param criteriaQuery The {@code CriteriaQuery} for which the dialect is sought
	 * @param function The function for which to check support
	 *
	 * @return The {@code SpatialDialect} associated with the specified {@code CriteriaQuery}
	 *
	 * @throws HibernateException If the dialect for the specified {@code CriteriaQuery} is not a {@code SpatialDialect}.
	 * or the specified {@code SpatialFunction} is not supported by the dialect.
	 */
	public static SpatialDialect getSpatialDialect(CriteriaQuery criteriaQuery, SpatialFunction function) {
		final Dialect dialect = criteriaQuery.getFactory().getDialect();
		if ( !( dialect instanceof SpatialDialect ) ) {
			throw new HibernateException( "A spatial expression requires a spatial dialect." );
		}
		final SpatialDialect spatialDialect = (SpatialDialect) dialect;
		if ( !spatialDialect.supports( function ) ) {
			throw new HibernateException( function + " function not supported by this dialect" );
		}
		return spatialDialect;
	}

	/**
	 * Determines the column name corresponding to the specified property path.
	 *
	 * @param propertyName The property path
	 * @param criteria The criteria
	 * @param criteriaQuery The criteria query
	 * @return The column name
	 * @throws HibernateException If the property could not be resolved, or more than one column is mapped by the property path.
	 */
	public static String findColumn(String propertyName, Criteria criteria, CriteriaQuery criteriaQuery) {
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		if ( columns.length != 1 ) {
			throw new HibernateException( "Spatial Expression may only be used with single-column properties" );
		}
		return columns[0];
	}
}
