/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.results;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class SimpleComposite {
	public String value1;
	public String value2;

	public SimpleComposite() {
	}

	public SimpleComposite(String value1, String value2) {
		this.value1 = value1;
		this.value2 = value2;
	}
}
