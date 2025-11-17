/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.criteria;

import org.locationtech.jts.geom.Geometry;

/**
 * @author Marco Belladelli
 */
public interface JTSSpatialCriteriaBuilder extends SpatialCriteriaBuilder<Geometry> {
}
