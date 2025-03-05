/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQuery;

public class Dummy {
	@Entity(name = "Inner")
	@NamedQuery(name = "allInner", query = "from Inner")
	public static class Inner extends Persona {
		@Id
		Integer id;

		String name;

		public Integer getId() {
			return id;
		}

		@Override
		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}
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

	@MappedSuperclass
	public abstract static class Persona {
		private String city;

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public abstract void setId(Integer id);

		public abstract String getName();

		public abstract void setName(String name);
	}
}
