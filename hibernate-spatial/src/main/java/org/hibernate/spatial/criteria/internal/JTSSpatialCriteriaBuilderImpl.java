/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.criteria.internal;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.spatial.criteria.JTSSpatialCriteriaBuilder;

import org.locationtech.jts.geom.Geometry;

/**
 * @author Marco Belladelli
 */
public class JTSSpatialCriteriaBuilderImpl extends SpatialCriteriaBuilderImpl<Geometry>
		implements JTSSpatialCriteriaBuilder {

	public JTSSpatialCriteriaBuilderImpl(HibernateCriteriaBuilder criteriaBuilder) {
		super( criteriaBuilder );
	}
}
