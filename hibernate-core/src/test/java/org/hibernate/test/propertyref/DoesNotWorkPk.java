/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
