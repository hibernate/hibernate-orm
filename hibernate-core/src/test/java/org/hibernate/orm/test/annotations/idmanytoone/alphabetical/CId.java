/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;
import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class CId implements Serializable {

	@ManyToOne
	@JoinColumn( name = "aId", nullable = false )
	private A parent;

	private int sequenceNumber;

	public CId() {
	}

	public A getParent() {
		return parent;
	}

	public void setParent(A parent) {
		this.parent = parent;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( parent == null ) ? 0 : parent.hashCode() );
		result = prime * result + sequenceNumber;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		final CId other = (CId) obj;
		if ( parent == null ) {
			if ( other.parent != null )
				return false;
		}
		else if ( !parent.equals( other.parent ) )
			return false;
		if ( sequenceNumber != other.sequenceNumber )
			return false;
		return true;
	}


}
