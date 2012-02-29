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


import org.hibernate.spatial.dialect.oracle.criterion.OracleSpatialAggregate;

/**
 * Provides Aggregate type spatial function interpretation
 */
class SpatialAggregate {

	boolean _aggregateType;

	String _aggregateSyntax;

	private final String SDO_AGGR = "SDO_AGGR_";

	SpatialAggregate() {
	}

	SpatialAggregate(int aggregation) {

		String specificAggrSyntax;

		switch ( aggregation ) {
			case org.hibernate.spatial.SpatialAggregate.EXTENT:
				specificAggrSyntax = "MBR";
				_aggregateType = false;
				break;
			case OracleSpatialAggregate.LRS_CONCAT:
				specificAggrSyntax = "LRS_CONCAT";
				_aggregateType = true;
				break;
			case OracleSpatialAggregate.CENTROID:
				specificAggrSyntax = "CENTROID";
				_aggregateType = true;
				break;
			case OracleSpatialAggregate.CONCAT_LINES:
				specificAggrSyntax = "CONCAT_LINES";
				_aggregateType = false;
				break;
			case OracleSpatialAggregate.UNION:
				specificAggrSyntax = "UNION";
				_aggregateType = true;
				break;
			case OracleSpatialAggregate.CONVEXHULL:
				specificAggrSyntax = "CONVEXHULL";
				_aggregateType = true;
				break;
			default:
				specificAggrSyntax = null;
				break;
		}
		if ( specificAggrSyntax != null ) {
			_aggregateSyntax = SDO_AGGR + specificAggrSyntax;
		}
	}

	public boolean isAggregateType() {
		return _aggregateType;
	}

	public String getAggregateSyntax() {
		return _aggregateSyntax;
	}

}
