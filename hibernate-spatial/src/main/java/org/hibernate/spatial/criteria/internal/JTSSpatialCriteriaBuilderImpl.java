/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
