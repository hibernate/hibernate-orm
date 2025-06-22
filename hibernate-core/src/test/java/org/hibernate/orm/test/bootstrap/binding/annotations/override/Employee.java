/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

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
