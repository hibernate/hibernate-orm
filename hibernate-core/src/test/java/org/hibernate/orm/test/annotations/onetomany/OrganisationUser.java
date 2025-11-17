/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@PrimaryKeyJoinColumn( name = "id_organisation_user" )
@Table( name = "ORGANISATION_USER" )
public class OrganisationUser extends Person implements Serializable {

	private String someText;
	private Organisation organisation;

	public OrganisationUser() {
	}

	public void setSomeText(String someText) {
		this.someText = someText;
	}

	@Column( name = "some_text", length=1024)
	public String getSomeText() {
		return someText;
	}

	public void setOrganisation(Organisation organisation) {
		this.organisation = organisation;
	}

	@ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
	@JoinColumn( name = "fk_id_organisation", nullable = false )
	public Organisation getOrganisation() {
		return organisation;
	}

}
