/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.compositeid;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

public class CompositeMoreFieldsId implements Serializable {

	private Long id;
	private Long anotherId;
	private Long generatedId;

	public CompositeMoreFieldsId(Long id, Long anotherId, Long generatedId) {
		this.id = id;
		this.anotherId = anotherId;
		this.generatedId = generatedId;
	}

	public CompositeMoreFieldsId(Long id, Long anotherId) {
		this.id = id;
		this.anotherId = anotherId;
	}

	private CompositeMoreFieldsId() {
	}

	public Long getId() {
		return id;
	}

	public Long getAnotherId() {
		return anotherId;
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
		CompositeMoreFieldsId that = (CompositeMoreFieldsId) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( anotherId, that.anotherId ) &&
				Objects.equals( generatedId, that.generatedId );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, anotherId, generatedId );
	}

	@Override
	public String toString() {
		return new StringJoiner( ", ", CompositeMoreFieldsId.class.getSimpleName() + "[", "]" )
				.add( "id=" + id )
				.add( "anotherId=" + anotherId )
				.add( "generatedId=" + generatedId )
				.toString();
	}
}
