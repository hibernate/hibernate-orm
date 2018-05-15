/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Being.java 18260 2009-12-17 21:14:07Z hardy.ferentschik $

package org.hibernate.test.annotations.access.jpa;
import javax.persistence.Entity;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Horse extends Animal {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
