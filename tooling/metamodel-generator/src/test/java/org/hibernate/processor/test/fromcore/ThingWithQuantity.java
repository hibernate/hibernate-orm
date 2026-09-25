package org.hibernate.processor.test.fromcore;

import jakarta.persistence.Entity;

/**
 * @author James Gilbertson
 */
@Entity
public class ThingWithQuantity extends Thing {
	private int quantity;

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
