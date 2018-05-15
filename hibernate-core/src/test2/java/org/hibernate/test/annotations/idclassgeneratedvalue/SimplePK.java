/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.idclassgeneratedvalue;
import java.io.Serializable;

/**
 * A SimplePK.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
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
