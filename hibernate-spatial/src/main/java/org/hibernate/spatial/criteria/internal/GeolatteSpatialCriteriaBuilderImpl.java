/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.criteria.internal;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.spatial.criteria.GeolatteSpatialCriteriaBuilder;

import org.geolatte.geom.Geometry;

/**
 * @author Marco Belladelli
 */
public class GeolatteSpatialCriteriaBuilderImpl extends SpatialCriteriaBuilderImpl<Geometry<?>>
		implements GeolatteSpatialCriteriaBuilder {

	public GeolatteSpatialCriteriaBuilderImpl(HibernateCriteriaBuilder criteriaBuilder) {
		super( criteriaBuilder );
	}
}
