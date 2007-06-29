//$Id: Mammal.java 6005 2005-03-04 11:41:11Z oneovthafew $
package org.hibernate.test.hql;

import java.util.Date;

/**
 * @author Gavin King
 */
public class Mammal extends Animal {
	private boolean pregnant;
	private Date birthdate;

	public boolean isPregnant() {
		return pregnant;
	}

	public void setPregnant(boolean pregnant) {
		this.pregnant = pregnant;
	}

	public Date getBirthdate() {
		return birthdate;
	}
	

	public void setBirthdate(Date birthdate) {
		this.birthdate = birthdate;
	}
	
}
