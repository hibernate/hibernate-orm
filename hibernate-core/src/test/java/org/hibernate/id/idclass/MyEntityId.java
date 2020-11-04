/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.idclass;

import java.io.Serializable;
import java.util.Objects;

public class MyEntityId implements Serializable {

	private Long idA;
	private Long idB;

	public MyEntityId(Long generatedId, Long providedId) {
		this.idA = generatedId;
		this.idB = providedId;
	}

	private MyEntityId() {
	}

	public Long getIdA() {
		return idA;
	}

	public void setIdA(Long idA) {
		this.idA = idA;
	}

	public Long getIdB() {
		return idB;
	}

	public void setIdB(Long idB) {
		this.idB = idB;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		MyEntityId pk = (MyEntityId) o;
		return Objects.equals( idA, pk.idA ) &&
				Objects.equals( idB, pk.idB );
	}

	@Override
	public int hashCode() {
		return Objects.hash( idA, idB );
	}
}
