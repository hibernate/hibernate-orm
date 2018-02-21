/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;


import org.hibernate.spatial.dialect.oracle.criterion.OracleSpatialAggregate;

/**
 * Provides Aggregate type spatial function interpretation
 */
class SpatialAggregate {

	private static final String SDO_AGGR = "SDO_AGGR_";
	private boolean aggregateType;
	private String aggregateSyntax;

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
