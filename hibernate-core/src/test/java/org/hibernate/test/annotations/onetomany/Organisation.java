/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.onetomany;
import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

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
