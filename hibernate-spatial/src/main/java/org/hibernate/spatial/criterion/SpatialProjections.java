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
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.SimpleProjection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.SpatialAggregate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.type.Type;

/**
 * A factory for spatial projections.
 *
 * @author Karel Maesen
 */
public class SpatialProjections {

	private SpatialProjections() {
	}

	/**
	 * Applies an extent projection to the specified geometry function
	 * <p>
	 * <p>The extent of a set of {@code Geometry}s is the union of their bounding boxes.</p>
	 *
	 * @param propertyName The property to use for calculating the extent
	 *
	 * @return an extent-projection for the specified property.
	 */
	public static Projection extent(final String propertyName) {
		return new SimpleProjection() {

			public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
				return new Type[] {
						criteriaQuery.getType( criteria, propertyName )
				};
			}

			public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery)
					throws HibernateException {
				final StringBuilder stbuf = new StringBuilder();

				final SessionFactoryImplementor factory = criteriaQuery.getFactory();
				final String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, propertyName );
				final Dialect dialect = factory.getDialect();
				if ( dialect instanceof SpatialDialect ) {
					final SpatialDialect seDialect = (SpatialDialect) dialect;
					stbuf.append(
							seDialect.getSpatialAggregateSQL( columns[0], SpatialAggregate.EXTENT )
					);
					stbuf.append( " as y" ).append( position ).append( '_' );
					return stbuf.toString();
				}
				return null;
			}

		};

	}

}
