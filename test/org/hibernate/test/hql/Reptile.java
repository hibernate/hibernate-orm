//$Id$
package org.hibernate.test.hql;

/**
 * @author Gavin King
 */
public class Reptile extends Animal {
	private float bodyTemperature;
	public float getBodyTemperature() {
		return bodyTemperature;
	}
	public void setBodyTemperature(float bodyTemperature) {
		this.bodyTemperature = bodyTemperature;
	}
}
