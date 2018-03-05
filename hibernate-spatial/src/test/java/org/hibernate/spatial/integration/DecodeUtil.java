package org.hibernate.spatial.integration;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.spatial.dialect.db2.DB2SpatialDialect;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecoder;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
public class DecodeUtil {

	public static WktDecoder getWktDecoder(Dialect dialect) {
		WktDecoder decoder = null;
		if ( dialect instanceof AbstractHANADialect ) {
			decoder = Wkt.newDecoder( Wkt.Dialect.HANA_EWKT );
		}
		else if ( dialect instanceof DB2SpatialDialect ) {
			decoder = Wkt.newDecoder( Wkt.Dialect.DB2_WKT );
		}
		else {
			decoder = Wkt.newDecoder( Wkt.Dialect.POSTGIS_EWKT_1 );
		}
		return decoder;
	}
}
