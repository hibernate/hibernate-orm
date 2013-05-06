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

	private boolean aggregateType;

	private String aggregateSyntax;

	private static final String SDO_AGGR = "SDO_AGGR_";

	SpatialAggregate() {
	}

	SpatialAggregate(int aggregation) {

		String specificAggrSyntax;

		switch ( aggregation ) {
			case org.hibernate.spatial.SpatialAggregate.EXTENT:
				specificAggrSyntax = "MBR";
				aggregateType = false;
				break;
			case OracleSpatialAggregate.LRS_CONCAT:
				specificAggrSyntax = "LRS_CONCAT";
				aggregateType = true;
				break;
			case OracleSpatialAggregate.CENTROID:
				specificAggrSyntax = "CENTROID";
				aggregateType = true;
				break;
			case OracleSpatialAggregate.CONCAT_LINES:
				specificAggrSyntax = "CONCAT_LINES";
				aggregateType = false;
				break;
			case OracleSpatialAggregate.UNION:
				specificAggrSyntax = "UNION";
				aggregateType = true;
				break;
			case OracleSpatialAggregate.CONVEXHULL:
				specificAggrSyntax = "CONVEXHULL";
				aggregateType = true;
				break;
			default:
				specificAggrSyntax = null;
				break;
		}
		if ( specificAggrSyntax != null ) {
			aggregateSyntax = SDO_AGGR + specificAggrSyntax;
		}
	}

	public boolean isAggregateType() {
		return aggregateType;
	}

	public String getAggregateSyntax() {
		return aggregateSyntax;
	}

}
