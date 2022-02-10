/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author Andrea Boriero
 */
@Entity
@IdClass(EntityWithNotAggregateId.PK.class)
public class EntityWithNotAggregateId {

	@Id
	private Integer value1;

	@Id
	private String value2;

	private String data;

	public PK getId() {
		return new PK( value1, value2 );
	}

	public void setId(PK id) {
		this.value1 = id.getValue1();
		this.value2 = id.getValue2();
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public static class PK implements Serializable {
		private Integer value1;
		private String value2;

		public PK() {
		}

		public PK(Integer value1, String value2) {
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

			PK pk = (PK) o;

			if ( value1 != null ? !value1.equals( pk.value1 ) : pk.value1 != null ) {
				return false;
			}
			return value2 != null ? value2.equals( pk.value2 ) : pk.value2 == null;
		}

		@Override
		public int hashCode() {
			int result = value1 != null ? value1.hashCode() : 0;
			result = 31 * result + ( value2 != null ? value2.hashCode() : 0 );
			return result;
		}
	}
}
