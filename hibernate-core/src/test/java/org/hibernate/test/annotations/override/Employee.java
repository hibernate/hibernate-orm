package org.hibernate.test.annotations.override;

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

	@AssociationOverrides({
		@AssociationOverride(
				name = "social.website",
				joinTable = @JoinTable(
						name = "tbl_empl_sites",
						inverseJoinColumns = @JoinColumn(name = "to_website_fk")
				)
		),
		@AssociationOverride(
				name = "phoneNumbers",
				joinTable = @JoinTable(
						name = "tbl_empl_phone"
				)
		),
		@AssociationOverride(
			name="address",
			joinColumns = @JoinColumn(name="fld_address_fk")
		)
	})
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

