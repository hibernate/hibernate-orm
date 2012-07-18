//$Id$
package org.hibernate.jpa.test.ops;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mammal extends Animal {
	private int mamalNbr;

	public int getMamalNbr() {
		return mamalNbr;
	}

	public void setMamalNbr(int mamalNbr) {
		this.mamalNbr = mamalNbr;
	}
}
