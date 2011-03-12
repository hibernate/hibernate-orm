package org.hibernate.envers.test.integration.inheritance.single.notownedrelation;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

@Entity
@DiscriminatorValue("PersonalContact")
@Audited
public class PersonalContact extends Contact {
	private String firstname;

	public String getFirstname() {
		return firstname;
	}
    
    public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
}
