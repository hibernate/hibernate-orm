/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Chris Cranford
 */
@Entity
public class EmbeddedIdEntity {
	@EmbeddedId
	private EmbeddedIdEntityId id;
	private String data;

	public EmbeddedIdEntityId getId() {
		return id;
	}

	public void setId(EmbeddedIdEntityId id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Embeddable
	public static class EmbeddedIdEntityId implements Serializable {
		private Integer value1;
		private String value2;

		EmbeddedIdEntityId() {

		}

		public EmbeddedIdEntityId(Integer value1, String value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		public Integer getValue1() {
			return value1;
		}

		public void setValue1(Integer value1) {
			this.value1 = value1;
		}

		public String getValue2() {
			return value2;
		}

		public void setValue2(String value2) {
			this.value2 = value2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddedIdEntityId that = (EmbeddedIdEntityId) o;
			return Objects.equals( value1, that.value1 ) &&
					Objects.equals( value2, that.value2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( value1, value2 );
		}

		@Override
		public String toString() {
			return "EmbeddedIdEntityId{" +
					"value1=" + value1 +
					", value2='" + value2 + '\'' +
					'}';
		}
	}
}
