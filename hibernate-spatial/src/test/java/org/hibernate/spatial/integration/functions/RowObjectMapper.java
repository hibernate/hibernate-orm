/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration.functions;

import java.util.Arrays;
import java.util.Objects;

/**
 * Mapper to ensure that the results of the test queries can be compared for equality.
 * 
 * @param <T> the returned object by the test query
 */
public interface RowObjectMapper<T> {
	default Data apply(Object obj) {
		Object[] row = (Object[]) obj;
		return new Data( (Number) row[0], row[1] );
	}
}

class Data {
	final Number id;
	Object datum;

	Data(Number id, Object datum) {
		this.id = id;
		this.datum = datum;
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

	private boolean isEquals(Object thisDatum, Object thatDatum) {
		if ( thisDatum instanceof byte[] ) {
			if ( !( thatDatum instanceof byte[] ) ) {
				return false;
			}
			return Arrays.equals( (byte[]) thisDatum, (byte[]) thatDatum );
		}

		return Objects.equals( thisDatum, thatDatum );

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


