/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.criteria;

import org.geolatte.geom.Geometry;

/**
 * @author Marco Belladelli
 */
public interface GeolatteSpatialCriteriaBuilder extends SpatialCriteriaBuilder<Geometry<?>> {
}
