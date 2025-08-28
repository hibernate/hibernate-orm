/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.hana;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;

public class HANASpatialUtils {

	private static final int POSTGIS_SRID_FLAG = 0x20000000;

	private HANASpatialUtils(){
		//prevent instantiation of Utility class
	}

	@SuppressWarnings("resource")
	public static Geometry<?> toGeometry(ResultSet rs, String name) throws SQLException {
		ByteBuffer buffer = toByteBuffer( rs.getObject( name ) );

		if ( buffer == null ) {
			return null;
		}

		// Get table and column names from the result set metadata
		String tableName = null;
		String columnName = null;
		for ( int i = 1; i <= rs.getMetaData().getColumnCount(); i++ ) {
			if ( name.equals( rs.getMetaData().getColumnLabel( i ) ) ||
					name.toUpperCase( Locale.ENGLISH ).equals( rs.getMetaData().getColumnLabel( i ) ) ) {
				tableName = rs.getMetaData().getTableName( i );
				columnName = rs.getMetaData().getColumnName( i );
			}
		}

		assert tableName != null;
		assert columnName != null;

		// no table and/or column names found (
		if ( tableName.isEmpty() || columnName.isEmpty() ) {
			return toGeometry( buffer );
		}

		byte orderByte = buffer.get();
		int typeCode = (int) buffer.getUInt();

		Connection connection = rs.getStatement().getConnection();

		// Check if SRID is set
		if ( ( typeCode & POSTGIS_SRID_FLAG ) != POSTGIS_SRID_FLAG ) {
			// No SRID set => try to get SRID from the database
			try (PreparedStatement psSrid = connection
					.prepareStatement(
							"SELECT SRS_ID FROM SYS.ST_GEOMETRY_COLUMNS WHERE SCHEMA_NAME=CURRENT_SCHEMA AND TABLE_NAME=? AND COLUMN_NAME=?" )) {
				psSrid.setString( 1, tableName );
				psSrid.setString( 2, columnName );

				try (ResultSet rsSrid = psSrid.executeQuery()) {
					if ( rsSrid.next() ) {
						int crsId = rsSrid.getInt( 1 );
						buffer = addCrsId( buffer.toByteArray(), orderByte, typeCode, crsId );
					}
					else {
						// ignore
					}
				}
			}
		}

		return toGeometry( buffer );
	}

	private static ByteBuffer addCrsId(byte[] wkb, byte orderByte, int typeCode, int crsId) {
		// original capacity + 4 bytes for the CRS ID
		ByteBuffer buffer = ByteBuffer.allocate( wkb.length + 4 );
		buffer.setByteOrder( ByteOrder.valueOf( orderByte ) );

		// write byte order
		buffer.put( orderByte );

		// set SRID flag
		buffer.putUInt( typeCode | POSTGIS_SRID_FLAG );

		// write CRS ID
		buffer.putInt( crsId );

		// write remaining data
		for ( int i = 5; i < wkb.length; i++ ) {
			buffer.put( wkb[i] );
		}

		buffer.rewind();
		return buffer;
	}

	public static Geometry<?> toGeometry(Object obj) {
		return toGeometry( toByteBuffer( obj ) );
	}

	private static Geometry<?> toGeometry(ByteBuffer buffer) {
		if ( buffer == null ) {
			return null;
		}
		WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.HANA_EWKB );
		return decoder.decode( buffer );
	}

	private static ByteBuffer toByteBuffer(Object obj) {
		byte[] raw = null;
		if ( obj == null ) {
			return null;
		}
		if ( ( obj instanceof byte[] ) ) {
			raw = (byte[]) obj;
		}
		else if ( obj instanceof Blob ) {
			raw = toByteArray( (Blob) obj );
		}
		else {
			throw new IllegalArgumentException( "Expected byte array or BLOB" );
		}

		ByteBuffer buffer = ByteBuffer.from( raw );
		buffer.setByteOrder( ByteOrder.valueOf( raw[0] ) );
		return buffer;
	}

	private static byte[] toByteArray(Blob blob) {
		try {
			return blob.getBytes( 1, (int) blob.length() );
		}
		catch (SQLException e) {
			throw new RuntimeException( "Error on transforming blob into array.", e );
		}
	}

	public static byte[] toEWKB(Geometry<?> geometry) {
		WkbEncoder encoder = Wkb.newEncoder( Wkb.Dialect.HANA_EWKB );
		ByteBuffer bytes = encoder.encode( geometry, ByteOrder.NDR );
		return bytes.toByteArray();
	}
}
