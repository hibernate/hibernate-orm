package org.hibernate.spatial.integration;

import org.geolatte.geom.codec.WktDecodeException;
import org.geolatte.geom.codec.WktDecoder;

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.testing.TestDataElement;

import static org.hibernate.spatial.integration.DecodeUtil.getWktDecoder;

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
}
