/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table( name = "ORGANISATION" )
public class Organisation implements Serializable {

	private Long idOrganisation;
	private String name;
	private Set<OrganisationUser> organisationUsers;

	public Organisation() {
	}

	public void setIdOrganisation(Long idOrganisation) {
		this.idOrganisation = idOrganisation;
	}

	@Id
	@Column( name = "id_organisation", nullable = false )
	public Long getIdOrganisation() {
		return idOrganisation;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column( name = "name", nullable = false, length = 40 )
	public String getName() {
		return name;
	}

	public void setOrganisationUsers(Set<OrganisationUser> organisationUsers) {
		this.organisationUsers = organisationUsers;
	}

	@OneToMany( mappedBy = "organisation",
			fetch = FetchType.LAZY,
			cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
	@OrderBy( value = "firstName" )
	public Set<OrganisationUser> getOrganisationUsers() {
		return organisationUsers;
	}

}
