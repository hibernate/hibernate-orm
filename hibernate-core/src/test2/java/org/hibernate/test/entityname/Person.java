/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.entityname;
import java.util.HashSet;
import java.util.Set;
/**
 * 
 * @author stliu
 *
 */
public class Person {
	private Long id;
	private String Name;
	private Set cars = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public Set getCars() {
		return cars;
	}

	public void setCars(Set cars) {
		this.cars = cars;
	}

}
