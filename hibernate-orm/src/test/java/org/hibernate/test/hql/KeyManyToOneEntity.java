/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
