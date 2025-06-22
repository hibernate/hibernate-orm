/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration.functions;

import java.util.Arrays;
import java.util.Objects;

import org.geolatte.geom.GeometryEquality;
import org.geolatte.geom.jts.JTS;

/**
 * Mapper to ensure that the results of the test queries can be compared for equality.
 */
public class RowObjectMapper {

	final GeometryEquality geomEq;

	RowObjectMapper(GeometryEquality geomEq) {
		this.geomEq = geomEq;
	}

	Data apply(Object obj) {
		Object[] row = (Object[]) obj;
		return new Data( (Number) row[0], row[1], geomEq );
	}
}

class Data {
	final Number id;
	Object datum;
	final GeometryEquality geomEq;

	Data(Number id, Object datum, GeometryEquality geomEq) {
		this.id = id;
		this.datum = datum;
		this.geomEq = geomEq;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Data data = (Data) o;
		return Objects.equals( id.intValue(), data.id.intValue() ) && isEquals( datum, data.datum );
	}

	@SuppressWarnings("unchecked")
	private boolean isEquals(Object thisDatum, Object thatDatum) {
		if ( thisDatum instanceof byte[] ) {
			if ( !( thatDatum instanceof byte[] ) ) {
				return false;
			}
			return Arrays.equals( (byte[]) thisDatum, (byte[]) thatDatum );
		}
		if ( thisDatum instanceof org.geolatte.geom.Geometry ) {
			return this.geomEq.equals( asGeolatte( thisDatum ), asGeolatte( thatDatum ) );
		}

		if ( thisDatum instanceof org.locationtech.jts.geom.Geometry ) {
			return this.geomEq.equals( fromJts( thisDatum ), fromJts( thatDatum ) );
		}
		return Objects.equals( thisDatum, thatDatum );

	}

	@SuppressWarnings("rawtypes")
	private org.geolatte.geom.Geometry asGeolatte(Object obj) {
		return (org.geolatte.geom.Geometry) obj;
	}

	@SuppressWarnings("rawtypes")
	private org.geolatte.geom.Geometry fromJts(Object obj) {
		return JTS.from( (org.locationtech.jts.geom.Geometry) obj );
	}


	@Override
	public int hashCode() {
		return Objects.hash( id, datum );
	}

	@Override
	public String toString() {
		return "Data{" +
				"id=" + id +
				", datum=" + datum +
				" (" + datum.getClass().getCanonicalName() + ")" +
				'}';
	}
}
