/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
