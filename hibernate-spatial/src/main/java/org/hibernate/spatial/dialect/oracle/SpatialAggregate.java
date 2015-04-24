/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
