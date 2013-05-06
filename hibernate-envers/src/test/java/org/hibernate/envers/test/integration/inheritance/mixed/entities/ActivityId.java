package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class ActivityId implements Serializable {
	private Integer id;
	private Integer id2;

	public ActivityId() {
	}

	public ActivityId(int i, int i1) {
		id = i;
		id2 = i1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getId2() {
		return id2;
	}

	public void setId2(Integer id2) {
		this.id2 = id2;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) {
			return true;
		}
		if ( !(obj instanceof ActivityId) ) {
			return false;
		}
		ActivityId id = (ActivityId) obj;
		return getId().equals( id.getId() ) && getId2().equals( id.getId2() );
	}

	@Override
	public int hashCode() {
		int result = getId().hashCode();
		result = 31 * result + getId2().hashCode();
		return result;
	}
}
