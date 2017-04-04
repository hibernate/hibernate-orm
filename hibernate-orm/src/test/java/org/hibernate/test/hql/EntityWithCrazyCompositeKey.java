/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: EntityWithCrazyCompositeKey.java 6567 2005-04-27 17:41:57Z steveebersole $
package org.hibernate.test.hql;


/**
 * Implementation of EntityWithCrazyCompositeKey.
 *
 * @author Steve Ebersole
 */
public class EntityWithCrazyCompositeKey {
	private CrazyCompositeKey id;
	private String name;

	public CrazyCompositeKey getId() {
		return id;
	}

	public void setId(CrazyCompositeKey id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
