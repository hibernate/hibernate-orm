/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Part.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.mapcompelem;



/**
 * @author Gavin King
 */
public class Part {
	private String name;
	private String description;
	Part() {}
	public Part(String n, String pw) {
		name=n;
		description = pw;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String password) {
		this.description = password;
	}
	public boolean equals(Object that) {
		return ( (Part) that ).getName().equals(name);
	}
	public int hashCode() {
		return name.hashCode();
	}
	public String toString() {
		return name + ":" + description;
	}
}
