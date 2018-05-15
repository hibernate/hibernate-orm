/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.cascade;


/**
 * Parent, but with an assigned identifier.
 *
 * @author Steve Ebersole
 */
public class ParentAssigned {
	private Long id;
	private String name;
	private ParentInfoAssigned info;

	public ParentAssigned() {
	}

	public ParentAssigned(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ParentInfoAssigned getInfo() {
		return info;
	}

	public void setInfo(ParentInfoAssigned info) {
		this.info = info;
	}
}
