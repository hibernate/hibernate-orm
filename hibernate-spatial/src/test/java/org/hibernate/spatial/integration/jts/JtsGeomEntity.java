/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.jts;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.integration.GeomEntityLike;
import org.hibernate.spatial.testing.TestDataElement;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.geolatte.geom.codec.WktDecoder;
import org.geolatte.geom.jts.JTS;

import static org.hibernate.spatial.integration.DecodeUtil.getWktDecoder;

/**
 * Test class used in unit testing.
 */
@Entity
@Table(name = "geomtest")
public class JtsGeomEntity implements GeomEntityLike<Geometry> {


	@Id
	private Integer id;

	private String type;

	private Geometry geom;

	public static JtsGeomEntity createFrom(TestDataElement element, Dialect dialect) throws ParseException {
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

		if ( !id.equals( geomEntity.id ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}

}
