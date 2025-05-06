/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Person {
	@EmbeddedId
	private PersonId id;

	private String address;

	@Embeddable
	public static class PersonId {
		private String name;
		private String snn;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSnn() {
			return snn;
		}

		public void setSnn(String snn) {
			this.snn = snn;
		}
	}


}
