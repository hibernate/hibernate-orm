/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Human.java 4364 2004-08-17 12:10:32Z oneovthafew $
package org.hibernate.test.unionsubclass;


/**
 * @author Gavin King
 */
public class Human extends Being {
	private char sex;
	
	/**
	 * @return Returns the sex.
	 */
	public char getSex() {
		return sex;
	}
	/**
	 * @param sex The sex to set.
	 */
	public void setSex(char sex) {
		this.sex = sex;
	}
	public String getSpecies() {
		return "human";
	}

}
