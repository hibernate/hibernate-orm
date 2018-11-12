/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.identifier.composite;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Embeddable;

/**
 * @author Vlad Mihalcea
 */
//tag::identifiers-composite-generated-mapping-example[]
@Embeddable
class EventId implements Serializable {

	private Integer category;

	private Timestamp createdOn;

	//Getters and setters are omitted for brevity
//end::identifiers-composite-generated-mapping-example[]

	public Integer getCategory() {
		return category;
	}

	public void setCategory(Integer category) {
		this.category = category;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

//tag::identifiers-composite-generated-mapping-example[]
	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		EventId that = (EventId) o;
		return Objects.equals( category, that.category ) &&
				Objects.equals( createdOn, that.createdOn );
	}

	@Override
	public int hashCode() {
		return Objects.hash( category, createdOn );
	}
}
//end::identifiers-composite-generated-mapping-example[]

