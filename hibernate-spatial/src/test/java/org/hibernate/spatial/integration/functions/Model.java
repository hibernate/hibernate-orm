/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import java.util.function.Function;

import org.hibernate.spatial.testing.domain.GeomEntity;
import org.hibernate.spatial.testing.domain.JtsGeomEntity;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.jts.JTS;

/**
 * Captures the Geometry model dimension in the dynamic tests for spatial functions.
 * <p>
 * T
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
enum Model {

	JTSMODEL(
			JtsGeomEntity.class,
			JTS::to
	),
	GLMODEL(
			GeomEntity.class,
			geom -> geom
	);

	/**
	 * Test Entity class
	 */
	final Class<?> entityClass;


	/**
	 * How to translate from Geolatte Geometry class to the object class
	 * expected by the entity geom property
	 */
	final Function<Geometry, Object> from;


	Model(
			Class<?> entityClass,
			Function<Geometry, Object> from
	) {
		this.entityClass = entityClass;
		this.from = from;
	}

}
