/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Andrea Boriero
 */
@Entity
@DiscriminatorValue("SUB-SUB")
public class SubSubEntity extends SubEntity {
	private String SubSubString;

	public String getSubSubString() {
		return SubSubString;
	}

	public void setSubSubString(String subSubString) {
		SubSubString = subSubString;
	}
}
