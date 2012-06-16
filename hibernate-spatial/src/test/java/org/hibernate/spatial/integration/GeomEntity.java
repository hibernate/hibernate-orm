/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.integration;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import org.hibernate.annotations.Type;
import org.hibernate.spatial.testing.EWKTReader;
import org.hibernate.spatial.testing.TestDataElement;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Test class used in unit testing.
 */
@Entity
@Table(name="geomtest")
public class GeomEntity {


	@Id
	private Integer id;

	private String type;

	@Type(type="org.hibernate.spatial.GeometryType")
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

	public static GeomEntity createFrom(TestDataElement element) throws ParseException {
		EWKTReader reader = new EWKTReader();
		GeomEntity result = new GeomEntity();
		result.setId(element.id);
		Geometry geom = reader.read(element.wkt);
		geom.setSRID(element.srid);
		result.setGeom(geom);
		result.setType(element.type);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		GeomEntity geomEntity = (GeomEntity) o;

		if (id != geomEntity.id) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}
}
