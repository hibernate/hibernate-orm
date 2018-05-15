/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref.component.partial;


public class Identity {
	private String name;
	private String ssn;
	
	public String getSsn() {
		return ssn;
	}
	public void setSsn(String id) {
		this.ssn = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
