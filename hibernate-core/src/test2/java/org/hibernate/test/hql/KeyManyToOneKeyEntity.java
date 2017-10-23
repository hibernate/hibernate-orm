/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: KeyManyToOneKeyEntity.java 7091 2005-06-08 19:31:26Z steveebersole $
package org.hibernate.test.hql;


/**
 * Implementation of KeyManyToOneKeyEntity.
 *
 * @author Steve Ebersole
 */
public class KeyManyToOneKeyEntity {
	private Long id;
	private String name;

	public KeyManyToOneKeyEntity(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
