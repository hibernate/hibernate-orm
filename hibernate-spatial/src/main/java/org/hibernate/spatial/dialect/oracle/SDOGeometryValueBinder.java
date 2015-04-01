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

package org.hibernate.spatial.dialect.oracle;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.oracle.Encoders;
import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;
import org.geolatte.geom.codec.db.oracle.SDOGeometry;
import org.hibernate.HibernateException;
import org.hibernate.spatial.helper.FinderException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 8/22/11
 */
class SDOGeometryValueBinder<J> implements ValueBinder<J> {

    private static final String SQL_TYPE_NAME = "MDSYS.SDO_GEOMETRY";

	private final OracleJDBCTypeFactory typeFactory;
	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	public SDOGeometryValueBinder(JavaTypeDescriptor<J> javaTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor, OracleJDBCTypeFactory typeFactory) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.typeFactory = typeFactory;
	}

	@Override
	public void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.STRUCT, SQL_TYPE_NAME );
		}
		else {
			final Geometry geometry = javaTypeDescriptor.unwrap( value, Geometry.class, options );
			final Object dbGeom = toNative( geometry, st.getConnection() );
			st.setObject( index, dbGeom );
		}
	}

	public Object store(SDOGeometry geom, Connection conn) throws SQLException, FinderException {
		return typeFactory.createStruct( geom, conn );
	}

	private Object toNative(Geometry geom, Connection connection) {
        final SDOGeometry sdoGeom = Encoders.encode(geom);
        if (geom != null) {
            try {
                return store(sdoGeom, connection);
            } catch (SQLException e) {
                throw new HibernateException("Problem during conversion from JTS to SDOGeometry", e);
            } catch (FinderException e) {
                throw new HibernateException("OracleConnection could not be retrieved for creating SDOGeometry " +
                        "STRUCT", e);
            }
        } else {
            throw new UnsupportedOperationException("Conversion of " + geom.getClass().getSimpleName() + " to Oracle STRUCT not supported");
        }
    }

}
