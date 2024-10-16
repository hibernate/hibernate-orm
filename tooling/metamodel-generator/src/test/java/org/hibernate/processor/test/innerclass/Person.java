/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.innerclass;

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
