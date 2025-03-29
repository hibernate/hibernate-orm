/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class Location implements Serializable {
	public double longitude;
	public double latitude;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final Location location = (Location) o;

		if ( Double.compare( location.latitude, latitude ) != 0 ) return false;
		if ( Double.compare( location.longitude, longitude ) != 0 ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		long temp;
		temp = longitude != +0.0d ? Double.doubleToLongBits( longitude ) : 0L;
		result = (int) ( temp ^ ( temp >>> 32 ) );
		temp = latitude != +0.0d ? Double.doubleToLongBits( latitude ) : 0L;
		result = 29 * result + (int) ( temp ^ ( temp >>> 32 ) );
		return result;
	}
}
