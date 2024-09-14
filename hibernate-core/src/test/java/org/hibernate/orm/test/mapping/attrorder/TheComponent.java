/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.attrorder;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class TheComponent {
	private String nestedName;
	private String nestedAnything;

	public String getNestedName() {
		return nestedName;
	}

	public void setNestedName(String nestedName) {
		this.nestedName = nestedName;
	}

	public String getNestedAnything() {
		return nestedAnything;
	}

	public void setNestedAnything(String nestedAnything) {
		this.nestedAnything = nestedAnything;
	}
}
