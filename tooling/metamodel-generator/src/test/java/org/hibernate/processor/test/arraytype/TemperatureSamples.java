package org.hibernate.processor.test.arraytype;

import jakarta.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class TemperatureSamples {
	private Integer[] samples;

	public Integer[] getSamples() {
		return samples;
	}

	public void setSamples(Integer[] samples) {
		this.samples = samples;
	}
}
