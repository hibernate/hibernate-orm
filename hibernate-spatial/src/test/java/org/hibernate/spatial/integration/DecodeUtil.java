/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration;

import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.WktDecoder;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/02/2018.
 */
public class DecodeUtil {

	public static WktDecoder getWktDecoder(Dialect dialect) {
		WktDecoder decoder = null;
		if ( dialect instanceof HANADialect ) {
			decoder = Wkt.newDecoder( Wkt.Dialect.HANA_EWKT );
		}
		else if ( dialect instanceof DB2Dialect ) {
			decoder = Wkt.newDecoder( Wkt.Dialect.DB2_WKT );
		}
		else {
			decoder = Wkt.newDecoder( Wkt.Dialect.POSTGIS_EWKT_1 );
		}
		return decoder;
	}
}
