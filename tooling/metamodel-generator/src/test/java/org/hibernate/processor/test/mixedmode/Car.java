package org.hibernate.processor.test.mixedmode;

/**
 * @author Hardy Ferentschik
 */
public class Car extends Vehicle {
	private String make;

	public int getHorsePower() {
		return 0;
	}

	public void setHorsePower(int horsePower) {
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}
}
