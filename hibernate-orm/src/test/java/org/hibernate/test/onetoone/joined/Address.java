/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Address.java 9914 2006-05-09 09:37:18Z max.andersen@jboss.com $
package org.hibernate.test.onetoone.joined;


/**
 * @author Gavin King
 */
public class Address {
	public String entityName;
	public String street;
	public String state;
	public String zip;
	
	public String toString() {
		return this.getClass() + ":" + street;
	}
}
