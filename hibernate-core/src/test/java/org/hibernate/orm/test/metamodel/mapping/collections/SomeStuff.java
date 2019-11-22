/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import javax.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class SomeStuff {
	private String anAttribute;
	private String anotherAttribute;

	public SomeStuff() {
	}

	public SomeStuff(String anAttribute, String anotherAttribute) {
		this.anAttribute = anAttribute;
		this.anotherAttribute = anotherAttribute;
	}

	public String getAnAttribute() {
		return anAttribute;
	}

	public void setAnAttribute(String anAttribute) {
		this.anAttribute = anAttribute;
	}

	public String getAnotherAttribute() {
		return anotherAttribute;
	}

	public void setAnotherAttribute(String anotherAttribute) {
		this.anotherAttribute = anotherAttribute;
	}
}
