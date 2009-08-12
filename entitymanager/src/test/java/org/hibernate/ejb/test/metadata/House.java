package org.hibernate.ejb.test.metadata;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class House {
	private Key key;
	private String address1;

	@EmbeddedId
	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	public static class Key implements Serializable {
		private String uuid;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
	}
}
