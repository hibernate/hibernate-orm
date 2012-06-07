//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PropertyAsset extends Asset {
	private double price;

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}
}
