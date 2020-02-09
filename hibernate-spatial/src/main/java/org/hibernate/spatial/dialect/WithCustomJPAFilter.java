package org.hibernate.spatial.dialect;

/**
 * An Interface for {@code SpatialDialect}s that require a custom
 * rendering to JPAQL for the filter predicate
 *
 * Created by Karel Maesen, Geovise BVBA on 09/02/2020.
 */
public interface WithCustomJPAFilter {

	String filterExpression(String geometryParam, String filterParam);
}
