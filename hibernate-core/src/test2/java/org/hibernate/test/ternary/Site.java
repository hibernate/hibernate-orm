/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Site.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.ternary;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Site {
	private String name;
	private String description;
	private Set employees = new HashSet();
	private Set managers = new HashSet();

	Site() {}
	public Site(String name) {
		this.name=name;
	}
	public Set getManagers() {
		return managers;
	}
	public void setManagers(Set managers) {
		this.managers = managers;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Set getEmployees() {
		return employees;
	}
	public void setEmployees(Set employees) {
		this.employees = employees;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
