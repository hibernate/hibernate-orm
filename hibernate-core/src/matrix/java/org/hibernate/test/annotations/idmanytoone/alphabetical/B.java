//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass( BId.class )
public class B {

	@Id
	private C parent;

	@Id
	private int sequenceNumber;

	public B() {
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


}

