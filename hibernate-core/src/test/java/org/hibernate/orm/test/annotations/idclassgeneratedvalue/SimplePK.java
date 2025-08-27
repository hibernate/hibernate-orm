/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclassgeneratedvalue;
import java.io.Serializable;

/**
 * A SimplePK.
 *
 * @author Stale W. Pedersen
 */
public class SimplePK implements Serializable {
	private final Long id1;
	private final Long id2;

	private SimplePK() {
		id1 = null;
		id2 = null;
	}

	public SimplePK(Long id1, Long id2) {
		this.id1 = id1;
		this.id2 = id2;
	}

	public Long getId1() {
		return id1;
	}

	public Long getId2() {
		return id2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		SimplePK simplePK = (SimplePK) o;

		return id1.equals( simplePK.id1 )
				&& id2.equals( simplePK.id2 );
	}

	@Override
	public int hashCode() {
		int result = id1.hashCode();
		result = 31 * result + id2.hashCode();
		return result;
	}
}
