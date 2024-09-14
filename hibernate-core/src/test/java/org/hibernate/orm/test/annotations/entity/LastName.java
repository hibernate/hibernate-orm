/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

@Embeddable
public class LastName {
	@Convert( converter = ToUpperConverter.class )
	private String lastName;

	public String getName() {
		return lastName;
	}

	public void setName(String lowerCaseName) {
		this.lastName = lowerCaseName;
	}


}
