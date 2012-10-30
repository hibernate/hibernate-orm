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

package org.hibernate.spatial.dialect.postgis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;
import org.postgresql.util.PGobject;

import org.hibernate.spatial.GeometrySqlTypeDescriptor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class PGGeometryTypeDescriptor extends GeometrySqlTypeDescriptor {


	public static final PGGeometryTypeDescriptor INSTANCE = new PGGeometryTypeDescriptor();

	@Override
	public int getSqlType() {
		return Types.OTHER;
	}

	@Override
	public String getTypeName() {
		return "GEOMETRY";
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.POSTGIS_EWKB_1 );
				Geometry geometry = getJavaDescriptor().unwrap( value, Geometry.class, options );
				byte[] bytes = encoder.encode( geometry, ByteOrder.NDR ).toByteArray();
				st.setBytes( index, bytes );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( rs.getObject( name ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getObject( name ) ), options );
			}
		};
	}

	private Geometry toGeometry(Object object) {
		if ( object == null ) {
			return null;
		}
		ByteBuffer buffer = null;
		if ( object instanceof PGobject ) {
			buffer = ByteBuffer.from( ( (PGobject) object ).getValue() );
			WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.POSTGIS_EWKB_1 );
			return decoder.decode( buffer );
		}
		throw new IllegalStateException( "Received object of type " + object.getClass().getCanonicalName() );

	}
}
