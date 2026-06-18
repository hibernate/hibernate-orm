/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.compositekey;

import java.io.Serializable;

public class Child {
	private Id id;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public static class Id implements Serializable {
		private Parent parent;
		private Long number;

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public Long getNumber() {
			return number;
		}

		public void setNumber(Long number) {
			this.number = number;
		}
	}
}
