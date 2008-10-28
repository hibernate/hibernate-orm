//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

@Embeddable
public class BId implements Serializable {

	@ManyToOne
	@JoinColumns( {
	@JoinColumn( name = "aId", nullable = false ),
	@JoinColumn( name = "bSequenceNumber", nullable = false )
			} )
	private C parent;

	private int sequenceNumber;

	public BId() {
	}

	public C getParent() {
		return parent;
	}

	public void setParent(C parent) {
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
		final BId other = (BId) obj;
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
