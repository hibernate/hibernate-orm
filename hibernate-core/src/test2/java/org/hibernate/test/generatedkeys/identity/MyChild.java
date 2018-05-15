/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generatedkeys.identity;


/**
 * @author Steve Ebersole
 */
public class MyChild {
	private Long id;
	private String name;
	private MyEntity inverseParent;

	public MyChild() {
	}

	public MyChild(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MyEntity getInverseParent() {
		return inverseParent;
	}

	public void setInverseParent(MyEntity inverseParent) {
		this.inverseParent = inverseParent;
	}
}
