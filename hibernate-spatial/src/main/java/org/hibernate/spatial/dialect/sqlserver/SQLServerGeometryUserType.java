///*
// * $Id: SQLServerGeometryUserType.java 302 2011-05-07 18:23:11Z maesenka $
// *
// * This file is part of Hibernate Spatial, an extension to the
// * hibernate ORM solution for geographic data.
// *
// * Copyright © 2007-2010 Geovise BVBA
// *
// * This library is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 2.1 of the License, or (at your option) any later version.
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this library; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// *
// * For more information, visit: http://www.hibernatespatial.org/
// */
//
//package org.hibernatespatial.sqlserver;
//
//import java.sql.Blob;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.sql.Types;
//
//import com.vividsolutions.jts.geom.Geometry;
//import org.hibernatespatial.AbstractDBGeometryType;
//import org.hibernatespatial.sqlserver.convertors.Decoders;
//import org.hibernatespatial.sqlserver.convertors.Encoders;
//
///**
// * The <code>GeometryUserType</code> for Microsoft SQL Server (2008).
// *
// * @author Karel Maesen, Geovise BVBA.
// */
//public class SQLServerGeometryUserType extends AbstractDBGeometryType {
//
//	public Geometry convert2JTS(Object obj) {
//		byte[] raw = null;
//		if ( obj == null ) {
//			return null;
//		}
//		if ( ( obj instanceof byte[] ) ) {
//			raw = (byte[]) obj;
//		}
//		else if ( obj instanceof Blob ) {
//			raw = toByteArray( (Blob) obj );
//		}
//		else {
//			throw new IllegalArgumentException( "Expected byte array." );
//		}
//		return Decoders.decode( raw );
//	}
//
//	private byte[] toByteArray(Blob blob) {
//		try {
//			return blob.getBytes( 1, (int) blob.length() );
//		}
//		catch ( SQLException e ) {
//			throw new RuntimeException( "Error on transforming blob into array.", e );
//		}
//	}
//
//	public Object conv2DBGeometry(Geometry geom, Connection connection) {
//		if ( geom == null ) {
//			throw new IllegalArgumentException( "Null geometry passed." );
//		}
//		return Encoders.encode( geom );
//	}
//
//	public int[] sqlTypes() {
//		return new int[] { Types.ARRAY };
//	}
//}
