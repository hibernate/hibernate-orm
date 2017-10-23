/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;


@TypeDef(
		name = "upperCase",
		typeClass = CasterStringType.class,
		parameters = {
			@Parameter(name = "cast", value = "upper")
		}
)

@Embeddable
public class LastName {
	
	@Type(type="upperCase")
	private String lastName;

	public String getName() {
		return lastName;
	}

	public void setName(String lowerCaseName) {
		this.lastName = lowerCaseName;
	}
	
	
}
