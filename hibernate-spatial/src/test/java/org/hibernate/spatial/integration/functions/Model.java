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

@SuppressWarnings({ "unchecked", "rawtypes" })
enum Model {


	JTSMODEL(
			JtsGeomEntity.class,
			obj -> JTS.from( (org.locationtech.jts.geom.Geometry) obj ),
			geom -> (Object) JTS.to( geom )
	),
	GLMODEL(
			GeomEntity.class,
			obj -> (Geometry) obj,
			geom -> geom
	);

	final Class<?> entityClass;
	final Function<Object, Geometry> to;
	final Function<Geometry, Object> from;

	Model(
			Class<?> entityClass,
			Function<Object, Geometry> to,
			Function<Geometry, Object> from
	) {
		this.entityClass = entityClass;
		this.to = to;
		this.from = from;
	}

}
