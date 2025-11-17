/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.testing.datareader.TestDataElement;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecoder;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.Geometry;

import static org.hibernate.spatial.integration.DecodeUtil.getWktDecoder;

/**
 * Test class used in unit testing.
 */
@Entity
@Table(name = "jtsgeomtest")
public class JtsGeomEntity implements GeomEntityLike<Geometry> {


	@Id
	private Integer id;

	private String type;

	private Geometry geom;

	public static JtsGeomEntity createFrom(TestDataElement element, Dialect dialect) {
		WktDecoder decoder = getWktDecoder( dialect );
		Geometry geom = JTS.to( decoder.decode( element.wkt ) );
		JtsGeomEntity result = new JtsGeomEntity();
		result.setId( element.id );
		result.setGeom( geom );
		result.setType( element.type );
		return result;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Geometry getGeom() {
		return geom;
	}

	public void setGeom(Geometry geom) {
		this.geom = geom;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		JtsGeomEntity geomEntity = (JtsGeomEntity) o;

		return id.equals( geomEntity.id );
	}

	public void setGeomFromWkt(String wkt) {
		this.geom = JTS.to( Wkt.fromWkt( wkt ) );
	}

	@Override
	public int hashCode() {
		return id;
	}

}
