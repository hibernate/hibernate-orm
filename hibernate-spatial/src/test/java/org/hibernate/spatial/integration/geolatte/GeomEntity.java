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

import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.integration.GeomEntityLike;
import org.hibernate.spatial.testing.TestDataElement;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.WktDecodeException;
import org.geolatte.geom.codec.WktDecoder;

import static org.hibernate.spatial.integration.DecodeUtil.getWktDecoder;

/**
 * Test class used in unit testing.
 * <p>
 * Not that this is Entity class uses raw Geometries, because in test classes a wide variety of SRIDs and
 * coordinate spaces are mixed. (This creates notable problems for Oracle, which is very, very strict in what it accepts)
 */
@Entity
@Table(name = "geomtest")
public class GeomEntity implements GeomEntityLike<Geometry> {

	@Id
	private Integer id;
	private String type;
	private Geometry geom;

	static GeomEntity createFrom(TestDataElement element, Dialect dialect) throws WktDecodeException {
		WktDecoder decoder = getWktDecoder( dialect );
		Geometry geom = decoder.decode( element.wkt );
		GeomEntity result = new GeomEntity();
		result.setId( element.id );
		result.setGeom( geom );
		result.setType( element.type );
		return result;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public Geometry getGeom() {
		return geom;
	}

	@Override
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

		GeomEntity geomEntity = (GeomEntity) o;

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
