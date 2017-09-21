/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import java.sql.Blob;
import java.sql.SQLException;

import org.geolatte.geom.ByteBuffer;
import org.geolatte.geom.ByteOrder;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkb;
import org.geolatte.geom.codec.WkbDecoder;
import org.geolatte.geom.codec.WkbEncoder;

public class HANASpatialUtils {

	public static Geometry<?> toGeometry(Object obj) {
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

		WkbDecoder decoder = Wkb.newDecoder( Wkb.Dialect.HANA_EWKB );
		return decoder.decode( buffer );
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
