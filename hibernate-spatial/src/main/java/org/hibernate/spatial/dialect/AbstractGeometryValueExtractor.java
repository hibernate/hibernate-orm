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

package org.hibernate.spatial.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.hibernate.spatial.GeometrySqlTypeDescriptor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicExtractor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public abstract class AbstractGeometryValueExtractor<X> extends BasicExtractor<X> {

	// later this will need to be configurable. So define this only once for both
	// extractor and binder.
	private static GeometryFactory geometryFactory = new GeometryFactory();

	public AbstractGeometryValueExtractor(JavaTypeDescriptor<X> javaDescriptor, GeometrySqlTypeDescriptor typeDescriptor) {
		super( javaDescriptor, typeDescriptor );
	}

	protected GeometryFactory getGeometryFactory() {
		return geometryFactory;
	}

	@Override
	protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		Object geomObj = rs.getObject( name );
		return (X) toJTS( geomObj );

	}

	@Override
	protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
		Object geomObj = statement.getObject( index );
		return (X) toJTS( geomObj );
	}

	@Override
	protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		Object geomObj = statement.getObject( name );
		return (X) toJTS( geomObj );
	}

	abstract public Geometry toJTS(Object object);

}
