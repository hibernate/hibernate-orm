/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class DoesNotWorkPk implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "track_no")
	private String id1;

	@Column(name = "track_ext")
	private String id2;

	public String getId1() {
		return id1;
	}

	public void setId1(String id1) {
		this.id1 = id1;
	}

	public String getId2() {
		return id2;
	}

	public void setId2(String id2) {
		this.id2 = id2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id1 == null) ? 0 : id1.hashCode());
		result = prime * result + ((id2 == null) ? 0 : id2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof DoesNotWorkPk) ) {
			return false;
		}
		DoesNotWorkPk other = (DoesNotWorkPk) obj;
		if ( id1 == null ) {
			if ( other.id1 != null ) {
				return false;
			}
		}
		else if ( !id1.equals( other.id1 ) ) {
			return false;
		}
		if ( id2 == null ) {
			if ( other.id2 != null ) {
				return false;
			}
		}
		else if ( !id2.equals( other.id2 ) ) {
			return false;
		}
		return true;
	}

}
