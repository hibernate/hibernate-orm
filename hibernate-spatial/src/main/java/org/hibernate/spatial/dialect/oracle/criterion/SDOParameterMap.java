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
package org.hibernate.spatial.dialect.oracle.criterion;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the parameters that can be passed into Oracle's Spatial
 * Operators
 *
 * @author Karel Maesen
 */
public class SDOParameterMap {

	public static final String DISTANCE = "distance";

	public static final String SDO_BATCH_SIZE = "sdo_batch_size";

	public static final String SDO_NUM_RES = "sdo_num_res";

	public static final String UNIT = "unit";

	public static final String MIN_RESOLUTION = "min_resolution";

	public static final String MAX_RESOLUTION = "max_resolution";

	public static final String MASK = "mask";

	public static final String QUERYTYPE = "querytype";

	private Map<String, Object> params = new HashMap<String, Object>();

	public SDOParameterMap() {
	}

	public boolean isEmpty() {
		return this.params.isEmpty();
	}

	public void setDistance(Double distance) {
		if ( distance != null ) {
			params.put( DISTANCE, distance );
		}
	}

	public Double getDistance() {
		return (Double) params.get( DISTANCE );
	}

	public void removeDistance() {
		params.remove( DISTANCE );
	}

	public void setSdoBatchSize(Integer size) {
		if ( size != null ) {
			params.put( SDO_BATCH_SIZE, size );
		}
	}

	public Integer getSdoBatchSize() {
		return (Integer) params.get( SDO_BATCH_SIZE );
	}

	public void removeSdoBatchSize() {
		params.remove( SDO_BATCH_SIZE );
	}

	public void setSdoNumRes(Integer size) {
		if ( size != null ) {
			params.put( SDO_NUM_RES, size );
		}
	}

	public Integer getSdoNumRes() {
		return (Integer) params.get( SDO_NUM_RES );
	}

	public void removeSdoNumRes() {
		params.remove( SDO_NUM_RES );
	}

	public void setUnit(String unit) {
		if ( unit != null ) {
			this.params.put( UNIT, unit );
		}
	}

	public String getUnit() {
		return (String) this.params.get( UNIT );
	}

	public void removeUnit() {
		this.params.remove( UNIT );
	}

	public void setMaxResolution(Double res) {
		if ( res != null ) {
			params.put( MAX_RESOLUTION, res );
		}
	}

	public Double getMaxResolution() {
		return (Double) params.get( MAX_RESOLUTION );
	}

	public void removeMaxResolution() {
		params.remove( MAX_RESOLUTION );
	}

	public void setMinResolution(Double res) {
		if ( res != null ) {
			params.put( MIN_RESOLUTION, res );
		}
	}

	public Double getMinResolution() {
		return (Double) params.get( MIN_RESOLUTION );
	}

	public void removeMinResolution() {
		params.remove( MIN_RESOLUTION );
	}

	public void setMask(String mask) {
		if ( mask != null ) {
			this.params.put( MASK, mask );
		}
	}

	public String getMask() {
		return (String) this.params.get( MASK );
	}

	public void removeMask() {
		this.params.remove( MASK );
	}

	public void setQueryType(String queryType) {
		if ( queryType != null ) {
			this.params.put( QUERYTYPE, queryType );
		}
	}

	public void setQueryTypeToFilter() {
		this.params.put( QUERYTYPE, "FILTER" );
	}

	public String getQueryType() {
		return (String) this.params.get( QUERYTYPE );
	}

	public void removeQueryType() {
		this.params.remove( QUERYTYPE );
	}

	public String toQuotedString() {
		StringBuilder stb = new StringBuilder();
		if ( params.isEmpty() ) {
			return "";
		}
		stb.append( '\'' );
		for ( String paramName : params.keySet() ) {
			if ( params.get( paramName ) == null ) {
				continue;
			}
			stb.append( paramName ).append( "=" ).append( params.get( paramName ) )
					.append( " " );
		}
		stb.deleteCharAt( stb.length() - 1 );
		stb.append( '\'' );
		return stb.toString();
	}

}
