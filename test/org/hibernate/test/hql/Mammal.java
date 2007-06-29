//$Id$
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
