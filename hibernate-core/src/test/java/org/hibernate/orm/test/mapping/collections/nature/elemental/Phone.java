/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.nature.elemental;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
//tag::ex-collection-elemental-model[]
@Embeddable
public class Phone {

	private String type;

	@Column(name = "`number`")
	private String number;

	//Getters and setters are omitted for brevity

//end::ex-collection-elemental-model[]

	public Phone() {
	}

	public Phone(String type, String number) {
		this.type = type;
		this.number = number;
	}

	public String getType() {
		return type;
	}

	public String getNumber() {
		return number;
	}
//tag::ex-collection-elemental-model[]
}
//end::ex-collection-elemental-model[]
