/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.manytomany;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrea Boriero
 */
public class User implements Serializable {

	private Long id;
	private Set groups = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set getGroups() {
		return groups;
	}

	public void setGroups(Set groups) {
		this.groups = groups;
	}
}
