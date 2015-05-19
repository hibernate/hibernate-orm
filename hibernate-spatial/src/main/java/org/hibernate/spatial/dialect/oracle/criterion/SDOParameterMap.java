/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle.criterion;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the parameters that can be passed into Oracle's Spatial operators
 *
 * @author Karel Maesen
 */
public class SDOParameterMap {

	/**
	 * The distance parameter
	 */
	public static final String DISTANCE = "distance";
	/**
	 * The sdo_batch_size parameter
	 */
	public static final String SDO_BATCH_SIZE = "sdo_batch_size";
	/**
	 * The sdo_num_res parameter
	 */
	public static final String SDO_NUM_RES = "sdo_num_res";
	/**
	 * The unit parameter
	 */
	public static final String UNIT = "unit";
	/**
	 * The min_resolution parameter
	 */
	public static final String MIN_RESOLUTION = "min_resolution";
	/**
	 * The max_resolution parameter
	 */
	public static final String MAX_RESOLUTION = "max_resolution";
	/**
	 * The mask parameter
	 */
	public static final String MASK = "mask";
	/**
	 * The querytype parameter
	 */
	public static final String QUERYTYPE = "querytype";
	private Map<String, Object> params = new HashMap<String, Object>();

	/**
	 * Constructs an empty instance
	 */
	public SDOParameterMap() {
	}

	/**
	 * Checks whether this instance is empty
	 *
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty() {
		return this.params.isEmpty();
	}

	public Double getDistance() {
		return (Double) params.get( DISTANCE );
	}

	/**
	 * Adds the distance parameter with the specified value
	 *
	 * @param distance The value for the distance parameter
	 */
	public void setDistance(Double distance) {
		if ( distance != null ) {
			params.put( DISTANCE, distance );
		}
	}

	/**
	 * Removes the distance parameter
	 */
	public void removeDistance() {
		params.remove( DISTANCE );
	}

	public Integer getSdoBatchSize() {
		return (Integer) params.get( SDO_BATCH_SIZE );
	}

	/**
	 * Adds the sdo_batch_size parameter with the specified value
	 *
	 * @param size The value for the sdo_batch_size parameter
	 */
	public void setSdoBatchSize(Integer size) {
		if ( size != null ) {
			params.put( SDO_BATCH_SIZE, size );
		}
	}

	/**
	 * Removes the sdo_batch_size parameter
	 */
	public void removeSdoBatchSize() {
		params.remove( SDO_BATCH_SIZE );
	}

	public Integer getSdoNumRes() {
		return (Integer) params.get( SDO_NUM_RES );
	}

	/**
	 * Adds the sdo_num_res parameter with the specified value
	 *
	 * @param res The value for the sdo_num_res parameter
	 */
	public void setSdoNumRes(Integer res) {
		if ( res != null ) {
			params.put( SDO_NUM_RES, res );
		}
	}

	/**
	 * Removes the sdo_num_res parameter
	 */
	public void removeSdoNumRes() {
		params.remove( SDO_NUM_RES );
	}

	public String getUnit() {
		return (String) this.params.get( UNIT );
	}

	/**
	 * Adds the unit parameter with the specified value
	 *
	 * @param unit The value for the unit parameter
	 */
	public void setUnit(String unit) {
		if ( unit != null ) {
			this.params.put( UNIT, unit );
		}
	}

	/**
	 * Removes the unit parameter
	 */
	public void removeUnit() {
		this.params.remove( UNIT );
	}

	public Double getMaxResolution() {
		return (Double) params.get( MAX_RESOLUTION );
	}

	/**
	 * Adds the max_resolution parameter with the specified value
	 *
	 * @param res The value for the max_resolution parameter
	 */
	public void setMaxResolution(Double res) {
		if ( res != null ) {
			params.put( MAX_RESOLUTION, res );
		}
	}

	/**
	 * Removes the max_resolution parameter
	 */
	public void removeMaxResolution() {
		params.remove( MAX_RESOLUTION );
	}

	public Double getMinResolution() {
		return (Double) params.get( MIN_RESOLUTION );
	}

	/**
	 * Adds the min_resolution parameter with the specified value
	 *
	 * @param res The value for the min_resolution parameter
	 */
	public void setMinResolution(Double res) {
		if ( res != null ) {
			params.put( MIN_RESOLUTION, res );
		}
	}

	/**
	 * Removes the min_resolution parameter
	 */
	public void removeMinResolution() {
		params.remove( MIN_RESOLUTION );
	}

	public String getMask() {
		return (String) this.params.get( MASK );
	}

	/**
	 * Adds the mask parameter with the specified value
	 *
	 * @param mask The value for the mask parameter
	 */
	public void setMask(String mask) {
		if ( mask != null ) {
			this.params.put( MASK, mask );
		}
	}

	/**
	 * Removes the mask parameter
	 */
	public void removeMask() {
		this.params.remove( MASK );
	}

	/**
	 * Adds the querytype parameter with value "FILTER"
	 */
	public void setQueryTypeToFilter() {
		this.params.put( QUERYTYPE, "FILTER" );
	}

	public String getQueryType() {
		return (String) this.params.get( QUERYTYPE );
	}

	/**
	 * Adds the querytype parameter with the specified value
	 *
	 * @param queryType The value for the quertype parameter
	 */
	public void setQueryType(String queryType) {
		if ( queryType != null ) {
			this.params.put( QUERYTYPE, queryType );
		}
	}

	/**
	 * Removes the querytype parameter
	 */
	public void removeQueryType() {
		this.params.remove( QUERYTYPE );
	}

	/**
	 * Returns all parameters contained in this instance as a quoted String containing
	 * the <parameter name>=<parameter value> pairs separated by spaces.
	 *
	 * The return format is as expected by the various SDO_GEOMETRY spatial functions.
	 *
	 * @return String
	 */
	public String toQuotedString() {
		final StringBuilder stb = new StringBuilder();
		if ( params.isEmpty() ) {
			return "";
		}
		stb.append( '\'' );
		for ( Map.Entry<String, Object> kv : params.entrySet() ) {
			if ( kv.getValue() == null ) {
				continue;
			}
			stb.append( kv.getKey() ).append( "=" ).append( kv.getValue() ).append( " " );
		}
		stb.deleteCharAt( stb.length() - 1 );
		stb.append( '\'' );
		return stb.toString();
	}

}
