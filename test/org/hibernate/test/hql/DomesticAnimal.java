//$Id$
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
