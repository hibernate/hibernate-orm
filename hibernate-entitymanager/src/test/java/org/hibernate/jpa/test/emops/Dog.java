//$Id$
package org.hibernate.jpa.test.emops;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance( strategy = InheritanceType.JOINED )
public class Dog extends Pet {
	private int numBones;

	public int getNumBones() {
		return numBones;
	}

	public void setNumBones(int numBones) {
		this.numBones = numBones;
	}
}
