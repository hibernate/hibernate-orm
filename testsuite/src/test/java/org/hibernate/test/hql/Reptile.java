//$Id: Reptile.java 5686 2005-02-12 07:27:32Z steveebersole $
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
