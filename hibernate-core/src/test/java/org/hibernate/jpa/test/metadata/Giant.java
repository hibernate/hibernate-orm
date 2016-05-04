/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metadata;

import javax.persistence.Entity;

/**
 * @author Gail Badner
 */
@Entity(name="HomoGigantus")
public class Giant extends Person {
	private long height;

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}
}
