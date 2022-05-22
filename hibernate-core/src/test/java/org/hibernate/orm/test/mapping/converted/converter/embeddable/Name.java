/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.converted.converter.embeddable;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Name {
	private final String personal;
	private final String family;

	public Name(String personal, String family) {
		this.personal = personal;
		this.family = family;
	}

	public String getPersonal() {
		return personal;
	}

	public String getFamily() {
		return family;
	}
}
