/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
