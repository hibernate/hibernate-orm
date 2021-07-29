/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import java.util.Objects;
import java.util.function.Function;

public interface RowObjectMapper<T> {
	default Data apply(Object obj) {
		Object[] row = (Object[]) obj;
		return new Data( (Integer) row[0], (T) row[1] );
	}
}

class Data {
	final Integer id;
	Object datum;

	Data(Integer id, Object datum) {
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
		return Objects.equals( id, data.id ) && Objects.equals( datum, data.datum );
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
				'}';
	}
}


