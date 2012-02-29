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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.spatial.jts.JTS;
import org.hibernate.spatial.jts.mgeom.MGeometryFactory;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/19/12
 */
public abstract class AbstractGeometryValueBinder<X> extends BasicBinder<X> {

	public AbstractGeometryValueBinder(JavaTypeDescriptor<X> javaDescriptor, SqlTypeDescriptor sqlDescriptor) {
		super( javaDescriptor, sqlDescriptor );
	}

	protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
		Geometry jtsGeom = getJavaDescriptor().unwrap( value, Geometry.class, options );
		Object dbGeom = toNative( jtsGeom, st.getConnection() );
		st.setObject( index, dbGeom );
	}

	protected MGeometryFactory getGeometryFactory() {
		return JTS.getDefaultGeometryFactory();
	}

	protected abstract Object toNative(Geometry jtsGeom, Connection connection);
}
