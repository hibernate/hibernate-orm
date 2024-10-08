/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.innerclass;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

public class Dummy {
	@Entity
	public static class Inner {
		@Id
		Integer id;

		String name;
	}

	@Embeddable
	public static class DummyEmbeddable {
		private String name;
		private int value;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}
}
