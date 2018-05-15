/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: Department.java 4448 2004-08-28 02:29:05Z steveebersole $
package org.hibernate.test.filter;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of Department.
 *
 * @author Steve
 */
public class Department {
	private Long id;
	private String name;
	private Set salespersons = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getSalespersons() {
		return salespersons;
	}

	public void setSalespersons(Set salespersons) {
		this.salespersons = salespersons;
	}
}
