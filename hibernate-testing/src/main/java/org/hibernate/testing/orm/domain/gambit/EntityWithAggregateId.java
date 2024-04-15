/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class EntityWithAggregateId {
	private Key key;
	private String data;

	public EntityWithAggregateId() {
	}

	public EntityWithAggregateId(Key key, String data) {
		this.key = key;
		this.data = data;
	}

	@EmbeddedId
	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}


	@Embeddable
	public static class Key implements Serializable {
		private String value1;
		private String value2;

		public Key() {
		}

		public Key(String value1, String value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		public String getValue1() {
			return value1;
		}

		public void setValue1(String value1) {
			this.value1 = value1;
		}

		public String getValue2() {
			return value2;
		}

		public void setValue2(String value2) {
			this.value2 = value2;
		}
	}

}
