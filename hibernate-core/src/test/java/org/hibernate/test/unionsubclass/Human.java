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
