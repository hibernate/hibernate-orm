/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.compositeid;

import java.io.Serializable;
import java.util.Objects;

public class CompositeId implements Serializable {

	private Long id;
	private Long generatedId;

	public CompositeId(Long id, Long generatedId) {
		this.id = id;
		this.generatedId = generatedId;
	}

	public CompositeId(Long id) {
		this.id = id;
	}

	private CompositeId() {
	}

	public Long getId() {
		return id;
	}

	public Long getGeneratedId() {
		return generatedId;
	}

	public void setGeneratedId(Long generatedId) {
		this.generatedId = generatedId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		CompositeId that = (CompositeId) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( generatedId, that.generatedId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, generatedId );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "CompositeId{" );
		sb.append( "id=" ).append( id );
		sb.append( ", generatedId=" ).append( generatedId );
		sb.append( '}' );
		return sb.toString();
	}
}
