/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2014 Adtech Geospatial
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

package org.hibernate.spatial.testing.dialects.db2;

import org.hibernate.spatial.testing.SQLExpressionTemplate;
import org.hibernate.spatial.testing.TestDataElement;
import org.hibernate.spatial.testing.WktUtility;

/**
 * @author David Adler
 * creation-date: 5/22/2014
 */
public class DB2ExpressionTemplate implements SQLExpressionTemplate {

	final String SQL_TEMPLATE = "insert into geomtest (id, type, geom) values (%d, '%s', DB2GSE.ST_GEOMETRY('%s', %d))";

	public String toInsertSql(TestDataElement testDataElement) {
		String wkt = WktUtility.getWkt( testDataElement.wkt );
		int srid = WktUtility.getSRID( testDataElement.wkt );
		return String.format(
				SQL_TEMPLATE,
				testDataElement.id,
				testDataElement.type,
				wkt,
				srid
		);
	}


}
