//$Id: DomesticAnimal.java 4899 2004-12-06 14:17:24Z pgmjsd $
package org.hibernate.test.hql;


/**
 * @author Gavin King
 */
public class DomesticAnimal extends Mammal {
	private Human owner;

	public Human getOwner() {
		return owner;
	}

	public void setOwner(Human owner) {
		this.owner = owner;
	}
}
