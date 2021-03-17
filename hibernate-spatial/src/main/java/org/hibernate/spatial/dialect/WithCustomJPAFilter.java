/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect;

/**
 * An Interface for {@code SpatialDialect}s that require a custom
 * rendering to JPAQL for the filter predicate
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 09/02/2020.
 */
public interface WithCustomJPAFilter {

	String filterExpression(String geometryParam, String filterParam);
}
