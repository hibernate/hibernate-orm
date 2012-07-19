//$Id$
package org.hibernate.jpa.test.ops;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Reptile extends Animal {
	private float temperature;

	public float getTemperature() {
		return temperature;
	}

	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}
}
