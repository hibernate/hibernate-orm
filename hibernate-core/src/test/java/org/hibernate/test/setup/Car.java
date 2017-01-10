
package org.hibernate.test.setup;

/**
 * Class presents the Car db table.
 */
public class Car {
	
	protected Long id;

	protected Long quantity;
	
	/**
	 * Gets the id.
	 * 
	 * @return id the id
	 */
	public Long getId() {
		return this.id;
	}
	/**
	 * Sets the id.
	 * 
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	public Long getQuantity() {
		return quantity;
	}

	public void setQuantity(Long quantity) {
		this.quantity = quantity;
	}

	
}