/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.proxy;

import javax.persistence.Entity;

@Entity
public class Adult extends Person {

	public Adult() {
		someInitMethod();
	}

	@Override
	public void someInitMethod() {
	}
}
