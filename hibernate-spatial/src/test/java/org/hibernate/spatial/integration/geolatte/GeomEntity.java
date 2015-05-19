/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.geolatte;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.spatial.testing.TestDataElement;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecodeException;
import org.geolatte.geom.codec.WktDecoder;

/**
 * Test class used in unit testing.
 *
 * Not that this is Entity class uses raw Geometries, because in test classes a wide variety of SRIDs and
 * coordinate spaces are mixed. (This creates notable problems for Oracle, which is very, very strict in what it accepts)
 *
 */
@Entity
@Table(name = "geomtest")
public class GeomEntity {

	@Id
	private Integer id;

	private String type;

	private Geometry geom;

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

	public static GeomEntity createFrom(TestDataElement element) throws WktDecodeException {
		WktDecoder decoder = Wkt.newDecoder( Wkt.Dialect.POSTGIS_EWKT_1 );
		Geometry geom = decoder.decode( element.wkt );
		GeomEntity result = new GeomEntity();
		result.setId( element.id );
		result.setGeom( geom );
		result.setType( element.type );
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		GeomEntity geomEntity = (GeomEntity) o;

		if ( ! id.equals(geomEntity.id) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}
}
