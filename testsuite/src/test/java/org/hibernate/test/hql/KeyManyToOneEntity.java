// $Id: KeyManyToOneEntity.java 7091 2005-06-08 19:31:26Z steveebersole $
package org.hibernate.test.hql;

import java.io.Serializable;

/**
 * Implementation of KeyManyToOneEntity.
 *
 * @author Steve Ebersole
 */
public class KeyManyToOneEntity {
	private Id id;
	private String name;

	public KeyManyToOneEntity(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static class Id implements Serializable {
		private KeyManyToOneKeyEntity key1;
		private String key2;

		public Id(KeyManyToOneKeyEntity key1, String key2) {
			this.key1 = key1;
			this.key2 = key2;
		}

		public KeyManyToOneKeyEntity getKey1() {
			return key1;
		}

		public void setKey1(KeyManyToOneKeyEntity key1) {
			this.key1 = key1;
		}

		public String getKey2() {
			return key2;
		}

		public void setKey2(String key2) {
			this.key2 = key2;
		}
	}
}
