/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration;

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
public enum Model {

	JTSMODEL(
			JtsGeomEntity.class,
			JTS::to,
			org.locationtech.jts.geom.Geometry.class
	),
	GLMODEL(
			GeomEntity.class,
			geom -> geom,
			Geometry.class
	);

	/**
	 * Test Entity class
	 */
	public final Class<?> entityClass;


	/**
	 * How to translate from Geolatte Geometry  to the geometry type
	 * expected by the entity in this model
	 */
	public final Function<Geometry, Object> from;

	/**
	 * The geometry type in this model
	 */
	public final Class<?> geometryClass;


	Model(
			Class<?> entityClass,
			Function<Geometry, Object> from,
			Class<?> geometryClass
	) {
		this.entityClass = entityClass;
		this.from = from;
		this.geometryClass = geometryClass;
	}

}
