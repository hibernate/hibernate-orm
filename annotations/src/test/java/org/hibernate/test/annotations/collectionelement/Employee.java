package org.hibernate.test.annotations.collectionelement;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

@Entity
public class Employee {
   @Id
	int id;

/*	@AssociationOverride(
	 name="social.website",
	 joinTable=@JoinTable(
		 name="xxxwebsites",
		 joinColumns=@JoinColumn(name="id"),
		 inverseJoinColumns=@JoinColumn(name="id" )
	 )
	)

	@AssociationOverride(
	 name="social.website",
		joinColumns=@JoinColumn(name="id"))
*/

	@AssociationOverride(
	 name="social.website",
	 joinTable=@JoinTable(
		 name="xxxwebsites",
		 joinColumns=@JoinColumn(name=""),
		 inverseJoinColumns=@JoinColumn(name="id" )
	 )
	)
   @Embedded
	ContactInfo contactInfo;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ContactInfo getContactInfo() {
		return contactInfo;
	}

	public void setContactInfo(ContactInfo contactInfo) {
		this.contactInfo = contactInfo;
	}

}

