/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.domain;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
public interface GeomEntityLike<G> {

	Integer getId();

	void setId(Integer id);

	String getType();

	void setType(String type);

	G getGeom();

	void setGeom(G geom);

	void setGeomFromWkt(String wkt);
}
